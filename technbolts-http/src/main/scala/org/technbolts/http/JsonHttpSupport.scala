package org.technbolts.http

import org.codehaus.jackson.JsonNode
import org.apache.http.client.methods.HttpUriRequest

trait JsonHttpSupport extends JsonSupport with HttpRequestBuilder {
  def httpClient:HttpClientSupport = HttpClientSupport()

  def jsonGet   (url:String):Response[JsonNode] = jsonGet(url,classOf[JsonNode])
  def jsonGet[T](url:String, klazz:Class[T]):Response[T] = jsonCall(Get(url),klazz)
  def jsonPut   (url:String):Response[JsonNode] = jsonPut(url,classOf[JsonNode])
  def jsonPut[T](url:String, klazz:Class[T]):Response[T] = jsonCall(Put(url),klazz)

  def jsonCall   (method:HttpUriRequest):Response[JsonNode] = jsonCall(method, classOf[JsonNode])
  def jsonCall[T](method:HttpUriRequest, klazz:Class[T]):Response[T] = httpClient.call(method, jsonReader(klazz))
}