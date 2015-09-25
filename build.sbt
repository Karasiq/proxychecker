name := "proxychecker"

organization := "com.karasiq"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Spray" at "http://repo.spray.io"

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

scalacOptions ++= Seq("-optimize", "-deprecation")

libraryDependencies ++= {
  val sprayV = "1.3.3"
  val akkaV = "2.3.11"
  Seq(
    "org.scalaj" %% "scalaj-http" % "0.3.16",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-kernel" % akkaV,
    "com.typesafe.akka" %% "akka-contrib" % akkaV,
    "com.maxmind.geoip2" % "geoip2" % "2.1.0",
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing-shapeless2" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "io.spray" %% "spray-json" % "1.3.2",
    "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "com.github.karasiq" %% "mapdbutils" % "1.0",
    "org.mapdb" % "mapdb" % "2.0-beta7",
    "me.lessis" %% "retry" % "0.2.0"
  )
}

mainClass in Compile := Some("com.karasiq.proxychecker.webservice.ProxyCheckerBoot")

enablePlugins(AkkaAppPackaging)