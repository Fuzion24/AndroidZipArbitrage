package android.master.keys

import utils.ZipFile
import scala.util.Try

case class ApkInjector(file:ZipFile) {

  type Filename = String
  type FileBytes = Array[Byte]
}

object ApkInjector {
  def apply(originalAPK:String):Try[ApkInjector] = ZipFile(originalAPK).map{ ApkInjector(_) }
}

