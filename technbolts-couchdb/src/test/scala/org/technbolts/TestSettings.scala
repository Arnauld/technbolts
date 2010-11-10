package org.technbolts

import java.net.URL
import org.springframework.util.DefaultPropertiesPersister
import java.util.Properties

object TestSettings {

  import org.slf4j.{Logger, LoggerFactory}
  private val logger: Logger = LoggerFactory.getLogger(classOf[TestSettings])

  def apply() = new TestSettings(loadProperties(classOf[TestSettings].getResource("/test.properties")))
  def apply(url:URL) = new TestSettings(loadProperties(url))

  def loadProperties(url:URL) = {
    val loader = new DefaultPropertiesPersister
    val props = new Properties
    loader.load(props, url.openStream)
    props
  }
}

class TestSettings(properties:Properties) {
  def getProperty(key:String) = properties.getProperty(key)
}