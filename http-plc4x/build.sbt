import riot.riotctl.sbt.RiotCtl._

name := "http-plc4x"
scalaVersion := "2.12.8"
libraryDependencies ++= Seq(
  // RIoT minor releases are backwards-compatible:
  "org.riot-framework" % "riot-core" % "0.+",
  // This will be an HTTP server using JSON marshallling:
  "com.typesafe.akka" %% "akka-http"   % "10.1.10",
  "com.typesafe.akka" %% "akka-http-jackson" % "10.1.10",
  // Use PLC4X to access a Siemens S7 PLC:
  "org.apache.plc4x" % "plc4j-api" % "0.5.0",
  "org.apache.plc4x" % "plc4j-connection-pool" % "0.5.0",
  "org.apache.plc4x" % "plc4j-opm" % "0.5.0",
  "org.apache.plc4x" % "plc4j-driver-s7" % "0.5.0",
  // Choose an SLF4J implementation, for example Logback:
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging)
  .settings(
    mainClass in Compile := Some("HttpPlc4x"),

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
