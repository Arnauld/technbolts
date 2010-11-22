package org.technbolts.util

import java.lang.String

object Bytes {

  val UTF8: String = "utf-8"

  implicit def stringToBytes(value:String) = value.getBytes(UTF8)
  implicit def bytesToString(value:Array[Byte]) = new String(value,UTF8)

}