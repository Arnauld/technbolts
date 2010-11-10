package org.technbolts.couchdb

import org.specs.Specification

class CouchDBSpecs extends Specification with CouchDBSettings {

  "CouchDB " should {
    "provides a welcome message" in {
      val couchDB = newCouchDB
      val welcome = couchDB.welcome
      welcome.message must equalTo("Welcome")
      welcome.version must equalTo(couchDBVersion)
    }

    "provides the list of database names handled" in {
      val couchDB = newCouchDB
      val names = couchDB.databaseNames
      names must contain ("mc_callum")
    }

    "provides usage and misc stats" in {
      val couchDB = newCouchDB
      val stats = couchDB.stats
      stats must be_!=(null)
    }
  }

}