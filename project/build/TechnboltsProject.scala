import sbt._

class TechnboltsProject(info: ProjectInfo) extends ParentProject(info) {

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
  /* repositories */
  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
  val springMilestone = "Spring Framework Maven Milestone Repository" at "http://maven.springframework.org/milestone"

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
  /* dependencies */
  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
  object Version {
    val log4j = "1.2.15"
    val slf4j = "1.6.1"
    val scala = "2.8.0"
    val lift = "2.1-RC1"
    val jackson = "1.5.6"
    val spring = "3.0.4.RELEASE"
    val joda   = "1.6.2"
    object Commons {
      val IO = "1.4"
      val httpclient = "4.0.1"
    }
  }

  object Dependencies {
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /*     Json       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val jackson_core = "org.codehaus.jackson" % "jackson-core-asl" % Version.jackson % "compile"
    lazy val jackson_mapper = "org.codehaus.jackson" % "jackson-mapper-asl" % Version.jackson % "compile"

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Commons: Http, IO, ...       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    object Commons {
      lazy val httpClient = "org.apache.httpcomponents" % "httpclient" % Version.Commons.httpclient % "compile"
      lazy val io = "commons-io" % "commons-io" % Version.Commons.IO % "compile"
    }
    lazy val joda = "joda-time" % "joda-time" % Version.joda % "compile"

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /*     spring       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val spring_core = "org.springframework" % "spring-core" % Version.spring

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Test */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val jetty = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default"
    lazy val junit = "junit" % "junit" % "4.8.1" % "test->default"
    lazy val specs = "org.scala-tools.testing" % ("specs_" + Version.scala) % "1.6.5" % "test->default"

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* logs */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    lazy val slf4j = "org.slf4j" % "slf4j-api" % Version.slf4j % "compile"
    // log4j for logging during tests
    lazy val slf4j_log4j = "org.slf4j" % "slf4j-log4j12" % Version.slf4j % "test->default"
    lazy val log4j = "log4j" % "log4j" % Version.log4j % "test->default" intransitive
  }

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
  /* Subprojects */
  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

  lazy val technbolts_util = project("technbolts-util", "technbolts-util", new TechnboltsUtilProject(_))
  lazy val technbolts_http = project("technbolts-http", "technbolts-http", new TechnboltsHttpProject(_), technbolts_util)
  lazy val technbolts_couchdb = project("technbolts-couchdb", "technbolts-couchdb", new TechnboltsCouchDBProject(_), technbolts_http)
  lazy val technbolts_gridi = project("technbolts-gridi", "technbolts-gridi", new TechnboltsGridiProject(_), technbolts_util)
  lazy val technbolts_grisbi = project("technbolts-grisbi", "technbolts-grisbi", new TechnboltsGrisbiProject(_), technbolts_util)
  lazy val technbolts_shibi = project("technbolts-shibi", "technbolts-shibi", new TechnboltsShibiProject(_), technbolts_util)
  lazy val technbolts_usecases = project("technbolts-usecases", "technbolts-usecases", new TechnboltsUseCasesProject(_), technbolts_gridi, technbolts_util)

  class TechnboltsUtilProject(info: ProjectInfo) extends TechnboltsProject(info) {
  }

  class TechnboltsHttpProject(info: ProjectInfo) extends TechnboltsProject(info) {
    val httpClient = Dependencies.Commons.httpClient
    val jackson_core = Dependencies.jackson_core
    val jackson_mapper = Dependencies.jackson_mapper
    val commons_io = Dependencies.Commons.io
  }

  class TechnboltsCouchDBProject(info: ProjectInfo) extends TechnboltsProject(info) {
    val spring_core = Dependencies.spring_core % "test->default"
  }

  class TechnboltsGridiProject(info: ProjectInfo) extends TechnboltsProject(info) {
    val rabbitmq = "com.rabbitmq" % "amqp-client" % "2.1.1"
    //
  }

  class TechnboltsGrisbiProject(info: ProjectInfo) extends TechnboltsProject(info) {
    val joda = Dependencies.joda
    //
  }

  class TechnboltsShibiProject (info: ProjectInfo) extends TechnboltsProject(info) {
    val jruby = "org.jruby" % "jruby-complete" % "1.5.5"
  }

  class TechnboltsUseCasesProject(info: ProjectInfo) extends TechnboltsProject(info) with AkkaProject {
    //
    val spring_amqp_version = "1.0.0.M1"
    val spring_amqp   = "org.springframework.amqp" % "spring-amqp"   % spring_amqp_version
    val spring_rabbit = "org.springframework.amqp" % "spring-rabbit" % spring_amqp_version
    val spring_rabbit_admin = "org.springframework.amqp" % "spring-rabbit-admin" % spring_amqp_version
  }

  class TechnboltsProject(info: ProjectInfo) extends DefaultProject(info) {
    val junit = Dependencies.junit
    val specs = Dependencies.specs

    val slf4j = Dependencies.slf4j
    val slf4j_log4j = Dependencies.slf4j_log4j
    val log4j = Dependencies.log4j

  }
}
