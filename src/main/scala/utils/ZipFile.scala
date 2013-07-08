package utils

import java.io.{ByteArrayOutputStream, FileInputStream, File}
import java.util.zip.{ZipInputStream, ZipEntry, ZipOutputStream}
import scala.util.{Success, Failure, Try}
import scala.collection.mutable.ListBuffer

case class FileEntry(zEntry:ZipEntry, data:Array[Byte], hash:String)

object FileEntry{
  import FileHelper._
  def apply(file:File):FileEntry = {
    val data = readFile(file)
    FileEntry(new ZipEntry(file.getName),data,SHA1(data).toString)
  }
}

object OptionHelper {
  def optionToTry[T](opt:Option[T], failMessage:String):Try[T] =
    opt match {
      case Some(a) => Success(a)
      case None    => Failure(new Exception(failMessage))
    }

}
object  ZipFile {
  import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

  private val BUFFER_SIZE = 4096

  def apply(fileName:String):Try[ZipFile] =
    ZipFile(new ZipInputStream(new FileInputStream(new File(fileName))))

  def apply(file:File):Try[ZipFile] =
    ZipFile(new ZipInputStream(new FileInputStream(file)))

  def apply(file:Option[File]):Try[ZipFile] = OptionHelper.optionToTry(file,"No file given").flatMap(ZipFile(_))

  def apply(bytes: Array[Byte]): Try[ZipFile] = apply(new ZipInputStream(new ByteArrayInputStream(bytes)))

  def apply(zip: ZipInputStream): Try[ZipFile] = {
    Try {
      // Back to mutable land for a moment
      var entry:  Option[ZipEntry] = Option(zip.getNextEntry)
      val items = new ListBuffer[FileEntry]()
      val buffer: Array[Byte] = new Array[Byte](BUFFER_SIZE)
      while (entry.isDefined) {
        def readIter(acc: ByteArrayOutputStream = new ByteArrayOutputStream()): Array[Byte] = {
          val   read = zip.read(buffer, 0, BUFFER_SIZE)
          if   (read == -1) acc.toByteArray
          else { acc.write(buffer, 0, read); readIter(acc) }
        }
        val fileBytes = readIter()
        items.append(FileEntry(entry.get,fileBytes,SHA1(fileBytes).asString))
        entry = Option(zip.getNextEntry)
      }
      zip.close()
      if (items.size > 0) new ZipFile(items.toSeq)
      else throw new Exception("Invalid zip file")
    }
  }

  def BLANK = new ZipFile(Seq())
}

class ZipFile(wrapped: Seq[FileEntry]) extends Seq[FileEntry] {

  lazy val entriesByHash:Map[String,FileEntry] = wrapped.foldLeft(Map[String,FileEntry]()){(acc,f) => acc + (f.hash -> f)}

  def +(e: FileEntry) = new ZipFile(wrapped.+:(e))

  def length = wrapped.length

  def apply(idx:Int) = wrapped(idx)

  def iterator = wrapped.iterator

  def -(entryToRemove:String):ZipFile = this -- Set(entryToRemove)

  def --(entriesToRemove:Set[String]):ZipFile = {
    val entries = for{
      FileEntry(entry,data,hash) <- wrapped
      if(!entriesToRemove.contains(entry.getName))
    } yield { FileEntry(entry,data,hash)}
    new ZipFile(entries)
  }

  def normalizedAddition(entryToAdd:FileEntry):ZipFile =
   if(entriesByHash.contains(entryToAdd.hash)) this else
     new ZipFile(this + entryToAdd)

  def addFiles(files:Seq[File]):ZipFile =
    files.foldRight(this) { (f,z) =>
      z.normalizedAddition(FileEntry(f))
    }

  def mergeSecondaryZip(z:ZipFile):ZipFile =
    z.foldRight(this){ (fe,z) => z.normalizedAddition(fe) }

  def ++(entriesToAdd:Seq[FileEntry]):ZipFile =
    new ZipFile(entriesToAdd ++ wrapped)

  def getZipFileBytes:Array[Byte] = {
    val outStream = new ByteArrayOutputStream()
    val outFile = new ZipOutputStream(outStream)
    for{
      FileEntry(entry,data,hash) <- wrapped
    }{
      outFile.putNextEntry(new ZipEntry(entry.getName))
      outFile.write(data)
    }
    outFile.flush()
    outFile.close()
    outStream.toByteArray
  }
}
