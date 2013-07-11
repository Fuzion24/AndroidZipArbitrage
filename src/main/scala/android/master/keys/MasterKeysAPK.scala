package android.master.keys

import utils.{OptionHelper, ZipFile, FileEntry}
import scala.util.Try
import java.io.File

object MasterKeysAPK {
  def apply(file:File, original:Boolean):Try[MasterKeysAPK] =
    ZipFile(file) map (MasterKeysAPK(_,original))

  def apply(file:Option[File], original:Boolean):Try[MasterKeysAPK] =
    OptionHelper.optionToTry(file,"No file given").flatMap(MasterKeysAPK(_,original))
}
case class MasterKeysAPK(wrapped:Seq[FileEntry], origApp:Boolean = false) extends ZipFile(wrapped){
  private val originalFileNameSet =  wrapped.map(_.zEntry.getName)

  override def mergeZip(z:ZipFile):ZipFile =
    if(origApp){
      val filesWithoutNameCollisions =
         z.map(_.zEntry.getName).filter(fName => !originalFileNameSet.contains(fName))
      if(!filesWithoutNameCollisions.isEmpty)
        throw new Exception(s"The following files do not exist in the original apk and thus would break the signatures: $originalFileNameSet")
      else
        super.mergeZip(z)
    } else super.mergeZip(z)

}