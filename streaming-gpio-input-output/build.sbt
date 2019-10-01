import riot.riotctl.sbt.RiotCtl._

name := "my-riot-project"
scalaVersion := "2.12.8"
libraryDependencies ++= Seq(
  // RIoT minor releases are backwards-compatible:
  "org.riot-framework" % "riot-core" % "0.+",
  // Choose an SLF4J implementation, for example Logback:
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging)
  .settings(
    mainClass in Compile := Some("Application"),

    // Skip javadoc for this project:
    publishArtifact in (Compile, packageDoc) := false,

    // Deployment Targets (hostname, username, password):
    riotTargets := Seq(
      riotTarget("raspberrypi", "pi", "raspberry")
      ),

    // Port to use for remote debugging:
    riotDbgPort := 8000,

    // Packages and features needed by your code:
    riotPrereqs := "openjdk-8-jdk-headless wiringpi",
    riotRequiresI2C := false,
    riotRequiresSPI := false
  )
