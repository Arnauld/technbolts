package org.technbolts.http

import org.apache.http.client.methods._
import org.apache.http.HttpEntity

trait HttpRequestBuilder {
  protected def prepare[T<:HttpEntityEnclosingRequestBase](req: T, entity: HttpEntity): T = {
    if (entity != null)
      req.setEntity(entity)
    req
  }
  def kneadUrl(url: String) = url

  def Get(url: String):HttpGet = new HttpGet(kneadUrl(url))
  def Put(url: String, entity: HttpEntity):HttpPut = prepare(new HttpPut(kneadUrl(url)), entity)
  def Put(url: String):HttpPut = Put(kneadUrl(url),null)
  def Post(url: String, entity: HttpEntity):HttpPost = prepare(new HttpPost(kneadUrl(url)), entity)
  def Post(url: String):HttpPost = Post(kneadUrl(url),null)
  def Delete(url: String):HttpDelete = new HttpDelete(kneadUrl(url))
  def Head(url: String):HttpHead = new HttpHead(kneadUrl(url))
}
