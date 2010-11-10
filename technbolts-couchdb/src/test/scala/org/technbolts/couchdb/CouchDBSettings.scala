package org.technbolts.couchdb

import org.technbolts.TestSettings
import org.specs.Specification

trait CouchDBSettings { self:Specification =>

  def settings: TestSettings = TestSettings()

  def couchDBVersion = settings.getProperty("couchdb.version")
  
  def couchDBHost = settings.getProperty("couchdb.host") match {
      case x if(x.isEmpty) => CouchDB.DEFAULT_HOST
      case x => x
    }
  def couchDBPort = settings.getProperty("couchdb.port") match {
      case x if(x.isEmpty) => None
      case x => Some(Integer.parseInt(x))
    }

  def newCouchDB = {
    logger.info("CouchDB ~~> " + couchDBHost +" : " + couchDBPort)
    CouchDB(couchDBHost, couchDBPort)
  }
}