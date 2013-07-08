package utils

import java.io.{ByteArrayOutputStream, FileInputStream, File}
import java.util.zip.{ZipInputStream, ZipEntry, ZipOutputStream}
import scala.util.{Success, Failure, Try}

object  ZipFile {
  import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

  private val BUFFER_SIZE = 4096

  def apply(fileName:String):Try[ZipFile] = {
    ZipFile(new ZipInputStream(new FileInputStream(new File(fileName))))
  }

  def apply(file:File):Try[ZipFile] = {
    ZipFile(new ZipInputStream(new FileInputStream(file)))
  }

  def apply(bytes: Array[Byte]): Try[ZipFile] = apply(new ZipInputStream(new ByteArrayInputStream(bytes)))

  def apply(zip: ZipInputStream): Try[ZipFile] = {
    Try {
      // Back to mutable land for a moment
      var entry:  Option[ZipEntry] = Option(zip.getNextEntry)
      var map: Map[ZipEntry, Array[Byte]] = Map()
      val buffer: Array[Byte] = new Array[Byte](BUFFER_SIZE)
      while (entry.isDefined) {
        def readIter(acc: ByteArrayOutputStream = new ByteArrayOutputStream()): Array[Byte] = {
          val   read = zip.read(buffer, 0, BUFFER_SIZE)
          if   (read == -1) acc.toByteArray
          else { acc.write(buffer, 0, read); readIter(acc) }
        }
        map  += (entry.get -> readIter())
        entry = Option(zip.getNextEntry)
      }
      zip.close()
      if (map.size > 0) new ZipFile(map)
      else throw new Exception("Invalid zip file")
    }
  }

  def BLANK = new ZipFile(Map())
}

class ZipFile(wrapped: Map[ZipEntry, Array[Byte]]) extends Map[ZipEntry, Array[Byte]] {
  def get(key: ZipEntry) = wrapped.get(key)

  def iterator = wrapped.iterator

  def -(key: ZipEntry) = wrapped - key

  def +[B1 >: Array[Byte]](kv: (ZipEntry, B1)) = wrapped + kv

  lazy val entriesByName = wrapped.keys.map(_.getName).toSet

  // Get an entry by file name
  def entryByName(name: String): Option[ZipEntry]      = wrapped.keys.find(_.getName.equals(name))
  def bytesByName(name: String): Option[Array[Byte]]   = entryByName(name).flatMap(wrapped.get _)

  def removeEntries(entryToRemove:String):ZipFile = {removeEntries(Set(entryToRemove))}

  def removeEntries(entriesToRemove:Set[String]):ZipFile = {
    val entries = for{
      (entry,data) <- wrapped
      if(!entriesToRemove.contains(entry.getName))
    } yield { (entry,data)}
    new ZipFile(entries)
  }

  def addEntries(entriesToAdd:Map[String,Array[Byte]]):ZipFile = {
    val entrees =
      for {(name,data) <- entriesToAdd }
      yield { new ZipEntry(name) -> data }
    new ZipFile(entrees ++ wrapped)
  }

  def hasEntry(entryName:String):Boolean =  entriesByName.contains(entryName)


  def getZipFileBytes:Array[Byte] = {
    val outStream = new ByteArrayOutputStream()
    val outFile = new ZipOutputStream(outStream)
    for{
      (entry,data) <- wrapped
    }{
      outFile.putNextEntry(new ZipEntry(entry.getName))
      outFile.write(data)
    }
    outFile.flush()
    outFile.close()
    outStream.toByteArray
  }
}
