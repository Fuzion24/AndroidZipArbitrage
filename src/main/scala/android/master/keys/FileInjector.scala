package android.master.keys

import java.io.File
import utils.ZipFile
import scala.util.Try

case class ApkInjector(file:ZipFile) {

  type Filename = String
  type FileBytes = Array[Byte]

  def injectFiles(files:Map[Filename, FileBytes]):Array[Byte] = {

  }

  def injectAllFilesFrom(zip:ZipFile):Array[Byte] = {

  }

}

object ApkInjector {
  def apply(originalAPK:String):Try[ApkInjector] = ZipFile(originalAPK).map{ ApkInjector(_) }
}

