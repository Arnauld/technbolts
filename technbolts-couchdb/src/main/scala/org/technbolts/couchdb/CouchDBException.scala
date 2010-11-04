package org.technbolts.couchdb

class CouchDBException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this (message, null)
}

/* ~~~~~~~~~ Database ~~~~~~~~~ */

class DatabaseAccessException(message: String, cause: Throwable) extends CouchDBException(message, cause) {
  def this(message: String) = this (message, null)
}
class DatabaseCreationException(message: String, cause: Throwable) extends DatabaseAccessException(message, cause) {
  def this(message: String) = this (message, null)
}
class DatabaseInvalidNameException(message: String, cause: Throwable) extends DatabaseAccessException(message, cause) {
  def this(message: String) = this (message, null)
}
class DatabaseAlreadyExistsException(message: String, cause: Throwable) extends DatabaseCreationException(message, cause) {
  def this(message: String) = this (message, null)
}
class DatabaseNotFoundException(message: String, cause: Throwable) extends DatabaseAccessException(message, cause) {
  def this(message: String) = this (message, null)
}

/* ~~~~~~~~~ Document ~~~~~~~~~ */

class DocumentNotFoundException(message: String, cause: Throwable) extends CouchDBException(message, cause) {
  def this(message: String) = this (message, null)
}

class DocumentUpdateConflictException(message: String, cause: Throwable) extends CouchDBException(message, cause) {
  def this(message: String) = this (message, null)
}
