package org.technbolts.couchdb

import org.specs.Specification
import org.technbolts.TestSettings

class CouchDBSpecs extends Specification {

  val settings = TestSettings()

  "CouchDB " should {
    "provides a welcome message" in {
      val couchDB = CouchDB()
      val welcome = couchDB.welcome
      welcome.message must equalTo("Welcome")
      welcome.version must equalTo(settings.couchDBVersion)
    }

    "provides the list of database names handled" in {
      val couchDB = CouchDB()
      val names = couchDB.databaseNames
      names must contain ("mc_callum")
    }

    "provides usage and misc stats" in {
      val couchDB = CouchDB()
      val stats = couchDB.stats
      stats must be_!=(null)
    }
  }
}