package org.technbolts.gridi

import java.lang.String
import org.technbolts.util.Bytes
import org.slf4j.{LoggerFactory, Logger}

trait Converter[T] {
  def convert(delivery: Delivery): Option[T]

  def convertToBytes(value: T): Array[Byte]

  def text_?(): Boolean
}

class StringConverter extends Converter[String] {
  private val log: Logger = LoggerFactory.getLogger(classOf[StringConverter])

  def text_?() = true

  def convertToBytes(value: String) = Bytes.stringToBytes(value)

  def convert(delivery: Delivery) = try {
    Some(Bytes.bytesToString(delivery.body))
  } catch {
    case e: Throwable =>
      log.error("Failed to convert delivery: " + delivery, e)
      None
  }
}













