package org.technbolts.http

import org.apache.http.client.HttpClient
import org.apache.http.client.methods._
import org.apache.http.conn.scheme.{Scheme, PlainSocketFactory, SchemeRegistry}
import org.apache.http.params.{BasicHttpParams, HttpProtocolParams, HttpParams}
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.commons.io.IOUtils
import org.apache.http.util.EntityUtils
import java.io._
import org.apache.http.{HttpVersion, HttpResponse, HttpEntity}

case class Response[T](statusCode: Int, item: Option[T])

object HttpClientSupport {
  val MAX_TOTAL_CONNECTIONS = 100;

  def apply() = {
    new HttpClientSupport {
      private lazy val _httpClient = createHttpClient;
      override def httpClient = _httpClient
    }
  }

  def createHttpClient: HttpClient = {
    // Create and initialize HTTP parameters
    val params: HttpParams = new BasicHttpParams();
    ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

    val http = new Scheme("http", PlainSocketFactory.getSocketFactory(), 80);

    // Create and initialize scheme registry
    val schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(http);

    // Create an HttpClient with the ThreadSafeClientConnManager.
    // This connection manager must be used if more than one thread will
    // be using the HttpClient.
    val connectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);
    new DefaultHttpClient(connectionManager, params);
  }
}

trait HttpClientSupport {
  import org.slf4j.{Logger, LoggerFactory}
  private val logger: Logger = LoggerFactory.getLogger(classOf[HttpClientSupport])

  def httpClient:HttpClient

  def call[T](request: HttpUriRequest, unmarshaller: (Reader) => Option[T]): Response[T] = {
    logger.debug("REQUEST: {}", request.getRequestLine)

    try {
      val response: HttpResponse = httpClient.execute(request);
      val statusCode: Int = response.getStatusLine().getStatusCode();
      // Read the response body.
      val entity: HttpEntity = response.getEntity();
      val unmarshalled: Option[T] = fromResponseStream(entity, unmarshaller);
      Response(statusCode, unmarshalled);
    } catch {
      case e: HttpClientException => throw e
      case e: Exception => throw new HttpClientException(e);
    }
  }

  def withAttachmentAsStream[T](url: String, reader: (InputStream) => Option[T]): Option[T] = {
    val method = new HttpGet(url)

    val response: HttpResponse = httpClient.execute(method)
    response.getEntity() match {
      case null => None
      case entity: HttpEntity =>
        val is: InputStream = entity.getContent()
        val read = reader(is)
        IOUtils.closeQuietly(is)
        read
    }
  }

  private def fromResponseStream[T](entity: HttpEntity, unmarshaller: (Reader) => Option[T]): Option[T] = {
    val is: InputStream = entity.getContent()
    val reader: Reader = new InputStreamReader(is, EntityUtils.getContentCharSet(entity))
    val unmarshalled = unmarshaller(reader)
    IOUtils.closeQuietly(reader)
    entity.consumeContent() // finish
    unmarshalled
  }
}