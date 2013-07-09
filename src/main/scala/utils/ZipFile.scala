package utils

import java.io.{ByteArrayOutputStream, FileInputStream, File}
import org.apache.commons.compress.archivers.zip.{ZipArchiveOutputStream, ZipArchiveInputStream, ZipArchiveEntry}
import scala.util.Try
import scala.collection.mutable.ListBuffer
import org.apache.commons.compress.archivers.ArchiveEntry
import java.util.zip.ZipEntry
import scala.util.Failure
import scala.Some
import scala.util.Success

case class FileEntry(zEntry:ArchiveEntry, data:Array[Byte], hash:String) {
  def setStored(){
    import utils.CryptoHelper._
    val e = zEntry.asInstanceOf[ZipArchiveEntry]
      e.setMethod(ZipEntry.STORED)
      e.setSize(data.size)
      e.setCrc(crc32(data))
  }
}

object FileEntry{
  import FileHelper._
  def apply(file:File):FileEntry = {
    val data = readFile(file)
    FileEntry(new ZipArchiveEntry(file.getName),data,SHA1(data).toString)
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
    ZipFile(new ZipArchiveInputStream(new FileInputStream(new File(fileName))))

  def apply(file:File):Try[ZipFile] =
    ZipFile(new ZipArchiveInputStream(new FileInputStream(file)))

  def apply(file:Option[File]):Try[ZipFile] = OptionHelper.optionToTry(file,"No file given").flatMap(ZipFile(_))

  def apply(bytes: Array[Byte]): Try[ZipFile] = apply(new ZipArchiveInputStream(new ByteArrayInputStream(bytes)))

  def apply(zip: ZipArchiveInputStream): Try[ZipFile] = {
    Try {
      // Back to mutable land for a moment
      var entry:  Option[ArchiveEntry] = Option(zip.getNextEntry)
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

  def +(e: FileEntry) = new ZipFile(wrapped :+ e)

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

  private def setEntriesStored:ZipFile = {
    wrapped.foreach(_.setStored())
    this
  }

  def normalizedAddition(entryToAdd:FileEntry):ZipFile =
   if(entriesByHash.contains(entryToAdd.hash)) this else
     new ZipFile(this + entryToAdd)

  def addFiles(files:Seq[File]):ZipFile =
    files.foldLeft(this) { (z,f) =>
      z.normalizedAddition(FileEntry(f))
    }

  def mergeSecondaryZip(z:ZipFile):ZipFile =
    z.foldLeft(this){ (orig,fe) =>
      orig.normalizedAddition(fe)
    }

  def ++(entriesToAdd:Seq[FileEntry]):ZipFile =
    new ZipFile(entriesToAdd ++ wrapped)

  def getZipFileBytes:Array[Byte] = {
    val outStream = new ByteArrayOutputStream()
    val outFile = new ZipArchiveOutputStream(outStream)
    for{
      f @ FileEntry(entry,data,hash) <- wrapped
    }{
      outFile.putArchiveEntry(entry)
      outFile.write(data)
      outFile.closeArchiveEntry()
    }

    outFile.flush()
    outFile.close()
    outStream.toByteArray
  }
}
