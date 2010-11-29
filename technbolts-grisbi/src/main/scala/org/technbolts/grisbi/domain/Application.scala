package org.technbolts.grisbi.domain

case class ApplicationId(value:String) extends Id

class Application(val id:ApplicationId) extends HasId[ApplicationId] with HasAttributes
