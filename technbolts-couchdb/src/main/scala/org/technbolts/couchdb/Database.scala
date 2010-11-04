package org.technbolts.couchdb

import org.apache.http.HttpStatus

object Database {
  val DatabaseName = """[a-z][a-z0-9_\$\(\)\+\-\/ ]*""".r
  def isNameValid(name:String):Boolean = name match {
    case DatabaseName() => true
    case _ => false
  }
}

class Database(couchDB:CouchDB, name:String) {
  import org.slf4j.{Logger, LoggerFactory}
  private val logger: Logger = LoggerFactory.getLogger(classOf[Database])

  /**
   * Indicate whether or not the database exists
   * @throws DatabaseAccessException
   */
  def exists = {
    val res = couchDB.jsonGet("/"+name)
    res.statusCode match {
      case HttpStatus.SC_OK => true
      case HttpStatus.SC_NOT_FOUND => false
      case _ =>
        logger.debug("Database does not exists, response: "+res)
        false
    }
  }

  /**
   * Create a database with the specified name if it does not already exist,
   * otherwise throw a DatabaseCreationException
   * @return a handler on the created database
   * @throws DatabaseCreationException if the name is invalid, if the database
   *             already exists or cannot be created.
   */
  def create:Unit = {
    if(!Database.isNameValid(name))
      throw new DatabaseInvalidNameException("Invalid database name <"+name+">")

    val res = couchDB.jsonPut("/"+name)
    res.statusCode match {
      case HttpStatus.SC_CREATED => // yeah!
      case HttpStatus.SC_PRECONDITION_FAILED =>
        throw new DatabaseAlreadyExistsException("Failed to create database <"+name+">: "+res)
      case _ =>
        throw new DatabaseCreationException("Database <"+name+">: "+res)
    }
  }
}