package org.technbolts.grisbi.event

import org.slf4j.{LoggerFactory, Logger}
import org.technbolts.util.ReadWrite

class MemoryEventBus extends EventBus with ReadWrite {
  
  val log: Logger = LoggerFactory.getLogger(classOf[MemoryEventBus])

  private var handlers: List[EventHandler] = Nil

  def fireEvent(event: Event) = read { handlers.foreach(h => h.onEvent(event)) }

  def addHandler(handler: EventHandler) = write { handlers ::= handler }

  def removeHandler(handler: EventHandler) = write { handlers = handlers.filterNot(_ == handler) }

}