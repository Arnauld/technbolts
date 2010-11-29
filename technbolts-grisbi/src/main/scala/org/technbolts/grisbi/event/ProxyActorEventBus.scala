package org.technbolts.grisbi.event

import actors.Actor
import org.slf4j.{LoggerFactory, Logger}

object ProxyActor {
  def actorOf(underlying: EventHandler):ProxyActorEventHandler = new ProxyActorEventHandler(underlying)
  def actorOf(underlying: EventBus):ProxyActorEventBus = new ProxyActorEventBus(underlying)
}

class ProxyActorEventHandler(underlying: EventHandler) extends Actor with EventHandler {

  val log: Logger = LoggerFactory.getLogger(classOf[ProxyActorEventHandler])

  def act() {
    loop {
      react {
        case event:Event => underlying.onEvent(event)
        case unknown =>
          log.warn("Unknown handler event received: {}", unknown)
      }
    }
  }

  def onEvent(event: Event) = this ! event
}

class ProxyActorEventBus(underlying: EventBus) extends Actor with EventBus {

  val log: Logger = LoggerFactory.getLogger(classOf[ProxyActorEventBus])
  
  def fireEvent(event: Event) = this ! FireEvent(event)

  def addHandler(handler: EventHandler) = this ! AddHandler(handler)

  def removeHandler(handler: EventHandler) = this ! RemoveHandler(handler)

  def act() {
    loop {
      react {
        case AddHandler(handler) => underlying.addHandler(handler)
        case RemoveHandler(handler) => underlying.removeHandler(handler)
        case FireEvent(event) => underlying.fireEvent(event)
        case unknown =>
          log.warn("Unknown bus event received: {}", unknown)
      }
    }
  }
}

