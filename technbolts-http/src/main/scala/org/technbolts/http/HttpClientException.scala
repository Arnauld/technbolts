package org.technbolts.http

class HttpClientException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(cause: Throwable) = this (cause.getMessage, cause)
}