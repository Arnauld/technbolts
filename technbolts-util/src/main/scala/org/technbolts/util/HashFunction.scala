package org.technbolts.util

trait HashFunction {
  def hash(bytes:Array[Byte]):Long
}

object HashFunction {
  implicit def stringToBytes(value:String):Array[Byte] = value.getBytes("utf-8")

  val FNV_64_INIT = 0xcbf29ce484222325L;
	val FNV_64_PRIME = 0x100000001b3L;

	val FNV_32_INIT = 2166136261L;
	val FNV_32_PRIME = 16777619;

  def fnv1_64:HashFunction = new HashFunction {
    override def hash(bytes:Array[Byte]):Long =
      bytes.foldLeft(FNV_64_INIT)((acc,value) => (acc*FNV_64_PRIME)^value) & 0xffffffffL
  }

  def fnv1a_64:HashFunction = new HashFunction {
    override def hash(bytes:Array[Byte]):Long =
      bytes.foldLeft(FNV_64_INIT)((acc,value) => (acc^value)*FNV_64_PRIME) & 0xffffffffL
  }

  def fnv1_32:HashFunction = new HashFunction {
    override def hash(bytes:Array[Byte]):Long =
      bytes.foldLeft(FNV_32_INIT)((acc,value) => (acc*FNV_32_PRIME)^value) & 0xffffffffL
  }

  def fnv1a_32:HashFunction = new HashFunction {
    override def hash(bytes:Array[Byte]):Long =
      bytes.foldLeft(FNV_32_INIT)((acc,value) => (acc^value)*FNV_32_PRIME) & 0xffffffffL
  }

}