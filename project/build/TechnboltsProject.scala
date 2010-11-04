import sbt._

class TechnboltsProject(info: ProjectInfo) extends ParentProject(info) {

  
  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * dependencies
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
  object Version {
    val log4j = "1.2.15"
    val slf4j = "1.6.1"
    val scala = "2.8.0"
    val lift = "2.1-RC1"
    val jackson = "1.5.6"
    val spring = "3.0.4.RELEASE"
    object Commons {
		val IO = "1.4"
		val httpclient = "4.0.1"
	}
  }
  
  object Dependencies {
	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /*     Json       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
	lazy val jackson_core   = "org.codehaus.jackson" % "jackson-core-asl"   % Version.jackson % "compile"
	lazy val jackson_mapper = "org.codehaus.jackson" % "jackson-mapper-asl" % Version.jackson % "compile"
	
	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Commons: Http, IO, ...       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    object Commons {
	    lazy val httpClient = "org.apache.httpcomponents" % "httpclient" % Version.Commons.httpclient % "compile"
		lazy val io         = "commons-io"                % "commons-io" % Version.Commons.IO         % "compile"
	}
	
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /*     spring       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val spring_core = "org.springframework" % "spring-core" % Version.spring

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Test */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val jetty = "org.mortbay.jetty"        %  "jetty"                   % "6.1.22" % "test->default"
	lazy val junit = "junit"                    %  "junit"                   % "4.8.1"  % "test->default"
	lazy val specs = "org.scala-tools.testing"  % ("specs_" + Version.scala) % "1.6.5"  % "test->default"
	
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* logs */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val slf4j       = "org.slf4j" % "slf4j-api"    % Version.slf4j % "compile"
    <!-- log4j for logging during tests   -->
    lazy val slf4j_log4j = "org.slf4j" % "slf4j-log4j12" % Version.slf4j % "test->default"
	lazy val log4j       = "log4j"     % "log4j"         % Version.log4j % "test->default" intransitive
  }
  
  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Subprojects
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

  lazy val technbolts_util       = project("technbolts-util",    "technbolts-util",    new TechnboltsUtilProject(_))
  lazy val technbolts_http       = project("technbolts-http",    "technbolts-http",    new TechnboltsHttpProject(_), technbolts_util)
  lazy val technbolts_couchDB    = project("technbolts-couchdb", "technbolts-couchdb", new TechnboltsCouchDBProject(_), technbolts_http)

  class TechnboltsUtilProject(info: ProjectInfo) extends TechnboltsProject(info) {
  }
  
  class TechnboltsHttpProject(info: ProjectInfo) extends TechnboltsProject(info) {
	val httpClient = Dependencies.Commons.httpClient
	val jackson_core   = Dependencies.jackson_core
	val jackson_mapper = Dependencies.jackson_mapper
	val commons_io     = Dependencies.Commons.io
  }
  
  class TechnboltsCouchDBProject(info: ProjectInfo) extends TechnboltsProject(info) {
    val spring_core = Dependencies.spring_core % "test->default"
  }
  
  class TechnboltsProject(info: ProjectInfo) extends DefaultProject(info) {
    val junit = Dependencies.junit
	val specs = Dependencies.specs
  
    val slf4j = Dependencies.slf4j
	val slf4j_log4j = Dependencies.slf4j_log4j
	val log4j = Dependencies.log4j
	
  }
}
