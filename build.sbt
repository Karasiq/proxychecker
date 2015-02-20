name := "proxychecker"

organization := "com.karasiq"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Spray" at "http://repo.spray.io"

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

scalacOptions ++= Seq("-optimize", "-deprecation")

libraryDependencies ++= {
  val sprayV = "1.3.2"
  Seq(
    "org.scalaj" %% "scalaj-http" % "0.3.16",
    "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    "com.typesafe.akka" %% "akka-kernel" % "2.3.9",
    "com.typesafe.akka" %% "akka-contrib" % "2.3.9",
    "com.maxmind.geoip2" % "geoip2" % "2.1.0",
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing-shapeless2" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "io.spray" %% "spray-json" % "1.3.1",
    "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "com.karasiq" %% "mapdb-utils" % "1.0",
    "org.mapdb" % "mapdb" % "1.0.7",
    "me.lessis" %% "retry" % "0.2.0"
  )
}

mainClass in Compile := Some("com.karasiq.proxychecker.webservice.ProxyCheckerBoot")

enablePlugins(AkkaAppPackaging)