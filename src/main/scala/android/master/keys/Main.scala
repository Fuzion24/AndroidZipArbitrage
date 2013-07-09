package android.master.keys

import java.io.File
import utils.{FileEntry, ZipFile}
import scala.util.{Success, Failure}
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry

case class Config(origAPK:Option[File] = None,mergeZip:Option[File] = None, out:Option[File] = None, files: Seq[File] = Seq())

object Main extends App {

  val parser = new scopt.OptionParser[Config]("Android Master Keys") {
    head("Android Master Keys", "1.0")
    opt[File]('a', "apk") required() valueName("<file>") action { (x, c) =>
      c.copy(origAPK = Some(x)) } text("path to original APK")
    opt[File]('z', "zip") valueName("<file>") action { (x, c) =>
      c.copy(mergeZip = Some(x)) } text("Merge files from this zip into original APK")
    opt[File]('o', "out") valueName("<file>") action { (x, c) =>
      c.copy(out = Some(x)) } text("output APK path")
    //note("some notes.\n")
    arg[File]("<file>...") unbounded() optional() action { (x, c) =>
      c.copy(files = c.files :+ x) } text("Files to add")
    help("help") text("prints this usage text")
  }

  //TODO: Clean up this rat's nest
  parser.parse(args, Config()) map { config =>
    import utils.FileHelper._
    ZipFile(config.origAPK.get) match {
      case Success(z) =>  ZipFile(config.mergeZip) match {
        case Success(nZip) => config.out match {
          case Some(o) => writeFile(o.getAbsolutePath, nZip.mergeSecondaryZip(z).getZipFileBytes)
          case None =>    writeFile(config.origAPK.get.getName + "MASTER_KEY",z.mergeSecondaryZip(nZip).getZipFileBytes)
        }
        case Failure(e) => s"Zip to be merged failed with: ${e.getMessage}"
      }
      case Failure(e) => s"Invalid Zip: ${e.getMessage}"
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
