package org.technbolts.grisbi

object Env {
  var timeProvider:TimeProvider = new TimeProvider {}
  def currentTimeMillis = timeProvider.currentTimeMillis
}

trait TimeProvider {
  def currentTimeMillis = System.currentTimeMillis
}
