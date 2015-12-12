val commonSettings = Seq(
  organization := "com.karasiq",
  isSnapshot := false,
  version := "1.2.0-M1",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(
    "Spray" at "http://repo.spray.io",
    "softprops-maven" at "http://dl.bintray.com/content/softprops/maven",
    Resolver.sonatypeRepo("snapshots")
  )
)

val backendDeps = {
  val sprayV = "1.3.3"
  val akkaV = "2.3.9"
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
    "me.lessis" %% "retry" % "0.2.0",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
  )
}

lazy val backendSettings = Seq(
  name := "proxychecker",
  libraryDependencies ++= backendDeps,
  mainClass in Compile := Some("com.karasiq.proxychecker.webservice.ProxyCheckerBoot"),
  gulpAssets in Compile := file("webapp") / "webapp",
  gulpCompile in Compile <<= (gulpCompile in Compile).dependsOn(fullOptJS in Compile in frontend)
)

lazy val frontendSettings = Seq(
  name := "proxychecker-webapp",
  libraryDependencies ++= Seq(
    "com.greencatsoft" %%% "scalajs-angular" % "0.5",
    "com.lihaoyi" %%% "upickle" % "0.3.6",
    "org.scala-js" %%% "scalajs-dom" % "0.8.0"
  ),
  persistLauncher in Compile := true
)

lazy val frontend = Project("proxychecker-webapp", file("webapp"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings, frontendSettings)

lazy val backend = Project("proxychecker", file("."))
  .enablePlugins(GulpPlugin, JavaAppPackaging)
  .settings(commonSettings, backendSettings)