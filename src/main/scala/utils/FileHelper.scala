package utils

import java.io._

object FileHelper {
  def writeFile(filename:String, data:String) {
    using(new FileWriter(filename)){ _.write(data)}
  }

  def writeFile(filename:String, data:Array[Byte]) {
    using(new FileOutputStream(filename)){ _.write(data)}
  }

  def readFully(input:InputStream): Array[Byte] = {
    val oStream = new ByteArrayOutputStream
    val buffer = new Array[Byte](4096)
    Iterator.continually(input.read(buffer))
      .takeWhile(_ != -1)
      .foreach { oStream.write(buffer, 0 , _) }

    oStream.flush()
    oStream.toByteArray
  }

  def readFullyAndClose(input:InputStream): Array[Byte] = {
    val ret = readFully(input)
    try{ input.close() } catch { case e: Throwable => }
    ret
  }

  def copyStream(input:InputStream, output:OutputStream){
    val buffer = new Array[Byte](4096)
    Iterator.continually(input.read(buffer))
      .takeWhile(_ != -1)
      .foreach { output.write(buffer, 0 , _) }
  }

  //TODO: Fix this so it doesn't result in runtime reflection
  def using[A <: {def close()}, B](param: A)(f: A => B): B =
    try { f(param) } finally { param.close() }

  def readFile(fileName: String):Array[Byte] =  readFile(new File(fileName))
  def readFile(file: File):Array[Byte] = readFully(new FileInputStream(file))

  def filesInDirectory(folder:String):List[String] = new File(folder).listFiles().map(_.getName).toList

  def fileExists(file:String) = new File(file).exists()

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
  def recursiveFileListing(path:String):List[File] = {
    recursiveListFiles(new File(path)).toList.filter(_.isFile)
  }
  def makeFolder(folder:String) { new File(folder).mkdir() }
}