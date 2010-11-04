package org.technbolts.couchdb

import org.codehaus.jackson.JsonNode
import org.apache.http.HttpStatus
import org.codehaus.jackson.annotate.{JsonProperty, JsonCreator}
import org.technbolts.http.JsonSupport
import org.technbolts.http._
import org.technbolts.util.LangUtils

/**
 * Some factory helpers
 */
object CouchDB {
  val DEFAULT_HOST = "localhost";
  val DEFAULT_PORT = 5984;

  def apply() = new CouchDB(DEFAULT_HOST, DEFAULT_PORT)

  def apply(port: Int) = new CouchDB(DEFAULT_HOST, port)

  def apply(host: String) = new CouchDB(host, DEFAULT_PORT)

  def apply(host: String, port: Int) = new CouchDB(host, port)
}

case class Welcome @JsonCreator() (@JsonProperty("couchdb") message:String, @JsonProperty("version") version:String)
case class Stats  (underlying: JsonNode) {
  override def toString = JsonSupport.prettyToString(underlying)
}

/**
 * 
 */
class CouchDB(val host: String, val port: Int) extends JsonHttpSupport {

  import org.slf4j.{Logger, LoggerFactory}
  private val logger: Logger = LoggerFactory.getLogger(classOf[CouchDB])

  val rootUrl = "http://" + host + ":" + port
  override def kneadUrl(url:String) = if(LangUtils.isEmpty(url)) rootUrl
                                      else if(url.startsWith("/")) rootUrl+url
                                      else url


  /**
   * CouchDB's Welcome
   */
  def welcome: Welcome = {
    val res: Response[Welcome] = jsonGet(rootUrl, classOf[Welcome])
    logger.debug("Welcome: {}", res)
    res.item match {
      case None => Welcome("...", "n/a")
      case Some(welcome) => welcome
    }
  }

  /**
   * Return the list of database names handled by this instance.
   */
  def databaseNames:List[String] = {
    import JsonImplicits._
    val res = jsonGet("/_all_dbs")
    logger.debug("Database names: {}", res)
    res.item match {
      case None => List()
      case Some(node) => node
    }
  }

  /*
   *
   */
  def stats: Stats = {
    val res = jsonGet("/_stats")
    val stats = res.item match {
      case None => Stats(null)
      case Some(node) => Stats(node)
    }
    logger.debug("Stats: {}", stats)
    stats
  }

  def handleStatus(statusCode: Int, errorMessage: String): Unit = statusCode match {
    case HttpStatus.SC_NOT_FOUND => throw new DocumentNotFoundException(errorMessage);
    case HttpStatus.SC_CONFLICT => throw new DocumentUpdateConflictException(errorMessage);
    case HttpStatus.SC_OK => // nothing to do
    case HttpStatus.SC_CREATED => // nothing to do
    case _ => throw new CouchDBException(errorMessage);
  }
}

