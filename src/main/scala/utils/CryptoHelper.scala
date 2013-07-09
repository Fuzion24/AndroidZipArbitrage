package utils

import java.security.MessageDigest
import java.util.zip.CRC32

case class SHA1(hash:String)
case class MD5(hash:String)

abstract class HashAlg(val digestName: String) {

  class Hash(val bytes: Array[Byte]) {
    def asString = {
      asBytes
        .map("%02X" format _)
        .mkString
        .toLowerCase
    }

    def asBytes = {
      MessageDigest
        .getInstance(digestName)
        .digest(bytes)
    }
  }

  def apply(bytes: Array[Byte]) = new Hash(bytes)
}

object SHA1   extends HashAlg("SHA1")
object MD5    extends HashAlg("MD5")
object SHA256 extends HashAlg("SHA-256")

object CryptoHelper {

  def crc32(bytes: Array[Byte]): Long = crc32(bytes, 0, bytes.length)

  /**
   * Compute the CRC32 of the segment of the byte array given by the specificed size and offset
   * @param bytes The bytes to checksum
   * @param offset the offset at which to begin checksumming
   * @param size the number of bytes to checksum
   * @return The CRC32
   */
  def crc32(bytes: Array[Byte], offset: Int, size: Int): Long = {
    val crc = new CRC32()
    crc.update(bytes, offset, size)
    crc.getValue()
  }


  type Hash = Array[Byte]

  def hashData(data:Array[Byte]):(String,String) = (SHA1(data).asString, MD5(data).asString)

  def SHAHash(s:String):String  = SHA1(s.getBytes).asString

  def hashToBytes(data:Array[Byte]):(Array[Byte],Array[Byte]) = (SHA1(data).asBytes, MD5(data).asBytes)

  def hashToString(hash: Hash) = hash.map(_.formatted("%02X")).mkString

  def stringToHash(hex: String): Hash = {
    try {
      (for { i <- 0 to hex.length-1 by 2 if i > 0 || !hex.startsWith( "0x" )} yield hex.substring( i, i+2 ))
        .map( Integer.parseInt( _, 16 ).toByte ).toArray
    } catch {
      case e: NumberFormatException => Array()
    }
  }

}