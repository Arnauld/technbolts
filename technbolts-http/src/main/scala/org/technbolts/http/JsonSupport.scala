package org.technbolts.http

import org.codehaus.jackson.JsonNode
import java.io.{StringWriter, Reader}
import org.codehaus.jackson.map.{SerializationConfig, ObjectMapper}

trait JsonSupport {
  import org.slf4j.{Logger, LoggerFactory}
  private val logger: Logger = LoggerFactory.getLogger(classOf[JsonSupport])
  private val mapper = new ObjectMapper();

  /*
   * Json helpers
   */
  def jsonReader[T](clazz: Class[T]): (Reader) => Option[T] = (reader: Reader) => {
    mapper.readValue(reader, clazz) match {
      case null => None
      case node => Some(node)
    }
  }
}

object JsonImplicits {
  implicit def jsonNode2String(node: JsonNode): String = node.getValueAsText

  implicit def jsonNode2ListOfString(node: JsonNode): List[String] = {
    collection.JavaConversions.asIterable(node).map(_.getValueAsText).toList
  }
}

object JsonSupport {

  def apply() = new JsonSupport {}

  lazy val prettyPrintMapper:ObjectMapper = createPrettyPrintMapper

  def createPrettyPrintMapper:ObjectMapper = {
    val mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper
  }

  def prettyToString(node: JsonNode): String = {
    val writer = new StringWriter()
    prettyPrintMapper.writeValue(writer, node)
    writer.toString
  }
}


