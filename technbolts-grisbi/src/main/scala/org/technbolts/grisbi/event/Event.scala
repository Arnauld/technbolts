package org.technbolts.grisbi.event

import org.technbolts.grisbi.Env


trait Event {
  def timestamp: Long = Env.currentTimeMillis
}

object Implicits {
  implicit def functionToEventHandler(f:(Event)=>Any) = new EventHandler {
    def onEvent(event: Event):Unit = f(event)
  }
}

trait EventHandler {
  def onEvent(event:Event):Unit
}

trait EventBus {
  def addHandler(handler:EventHandler):Unit
  def removeHandler(handler:EventHandler):Unit
  def fireEvent(event:Event):Unit
}

sealed trait EventBusEvent
case class AddHandler(handler:EventHandler) extends EventBusEvent
case class RemoveHandler(handler:EventHandler) extends EventBusEvent
case class FireEvent(event:Event) extends EventBusEvent