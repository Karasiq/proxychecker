name := "proxychecker"

organization := "com.karasiq"

isSnapshot := true

version := "1.2-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Spray" at "http://repo.spray.io",
  "softprops-maven" at "http://dl.bintray.com/content/softprops/maven",
  Resolver.sonatypeRepo("snapshots")
)

scalacOptions ++= Seq("-optimize", "-deprecation")

libraryDependencies ++= {
  val sprayV = "1.3.3"
  val akkaV = "2.3.11"
  Seq(
    "org.scalaj" %% "scalaj-http" % "0.3.16",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-contrib" % akkaV,
    "com.maxmind.geoip2" % "geoip2" % "2.1.0",
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing-shapeless2" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "io.spray" %% "spray-json" % "1.3.2",
    "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "com.github.karasiq" %% "mapdbutils" % "1.1-SNAPSHOT",
    "org.mapdb" % "mapdb" % "2.0-beta8",
    "me.lessis" %% "retry" % "0.2.0"
  )
}

mainClass in Compile := Some("com.karasiq.proxychecker.webservice.ProxyCheckerBoot")

enablePlugins(JavaAppPackaging)

lazy val compileWebapp = taskKey[Unit]("Compiles web application")

compileWebapp in Compile := {
  import sys.process._
  assert(Seq("webapp/make.bat").! == 0, "Webapp compilation failed")
}

compile in Compile <<= (compile in Compile).dependsOn(compileWebapp in Compile)