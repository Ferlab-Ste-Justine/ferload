name := """ferload"""
organization := "bio.ferlab"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
val keycloakVersion = "17.0.1"

scalaVersion := "2.13.9"

libraryDependencies += guice
libraryDependencies += caffeine
libraryDependencies += ws
libraryDependencies += "software.amazon.awssdk" % "s3" % "2.17.295"
//libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.880"
libraryDependencies += "org.keycloak" % "keycloak-core" % keycloakVersion
libraryDependencies += "org.keycloak" % "keycloak-authz-client" % keycloakVersion
//libraryDependencies += "org.keycloak" % "keycloak-adapter-core" % keycloakVersion
libraryDependencies += "org.mockito" % "mockito-core" % "3.8.0" % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.3"

packageName in Universal := name.value
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "bio.ferlab.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "bio.ferlab.binders._"
