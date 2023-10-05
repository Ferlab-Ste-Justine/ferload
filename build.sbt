

resolvers += "Sonatype Releases" at "https://s01.oss.sonatype.org/content/repositories/releases/"
val tapirVersion = "1.7.3"
val http4sVersion = "0.23.23"
val sstpVersion = "3.9.0"
scalacOptions ++= Seq("-Xmax-inlines", "100")
lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "ferload",
    version := "0.1.0-SNAPSHOT",
    organization := "bio.ferlab",
    scalaVersion := "3.3.1",
    assembly / assemblyJarName := "ferload.jar",
    assembly / assemblyMergeStrategy:= {
      case PathList("META-INF", "mailcap") => MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") => MergeStrategy.first
      case PathList("module-info.class") => MergeStrategy.discard
      case x => MergeStrategy.defaultMergeStrategy(x)
    },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "com.softwaremill.sttp.client3" %% "http4s-backend" % sstpVersion,
      "com.softwaremill.sttp.client3" %% "circe" % sstpVersion,
      "software.amazon.awssdk" % "s3" % "2.20.150",
      "com.github.ben-manes.caffeine" % "caffeine" % "3.1.8",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % "1.7.5" % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % "1.7.5" % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    )

  )
)


