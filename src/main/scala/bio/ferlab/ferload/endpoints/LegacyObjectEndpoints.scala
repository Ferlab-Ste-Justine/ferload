package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.Config
import bio.ferlab.ferload.model.{ErrorResponse, ObjectUrl, User}
import bio.ferlab.ferload.services.{AuthorizationService, S3Service}
import cats.effect.IO
import cats.implicits.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*

object LegacyObjectEndpoints:


  private def securedGlobalEndpoint(authorizationService: AuthorizationService, resourceGlobalName: String): PartialServerEndpoint[String, User, Unit, (StatusCode, ErrorResponse), Unit, Any, IO] =
    endpoint
      .securityIn(auth.bearer[String]())
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .serverSecurityLogic(token => authorizationService.authLogic(token, Seq(resourceGlobalName)))

  private def objectByPath(authorizationService: AuthorizationService, resourceGlobalName: String): PartialServerEndpoint[String, User, List[String], (StatusCode, ErrorResponse), ObjectUrl, Any, IO] =
    securedGlobalEndpoint(authorizationService, resourceGlobalName)
      .get
      .description("Retrieve an object by its path and return an url to download it")
      .deprecated()
      .in(paths.description("Path of the object to retrieve"))
      .out(jsonBody[ObjectUrl])

  private def objectsByPaths(authorizationService: AuthorizationService, resourceGlobalName: String): PartialServerEndpoint[String, User, String, (StatusCode, ErrorResponse), Map[String, String], Any, IO] =
    securedGlobalEndpoint(authorizationService, resourceGlobalName)
      .description("Retrieve a list of objects by their paths and return a list of download URLs for each object")
      .deprecated()
      .post
      .in("downloadLinks")
      .in(stringBody.description("List of URLs of objects to retrieve").example("file1.vcf\nfile2.vcf"))
      .out(jsonBody[Map[String, String]]
        .description("List of files URLs by object path")
        .example(Map("file1.vcf" -> "https://file1.vcf", "file2.vcf" -> "https://file2.vcf"))
      )

  def objectByPathServer(authorizationService: AuthorizationService, s3Service: S3Service, resourceGlobalName: String, defaultBucket: String): ServerEndpoint[Any, IO] =
    objectByPath(authorizationService, resourceGlobalName).serverLogicSuccess { user =>
      file => s3Service.presignedUrl(defaultBucket, file.mkString("/")).pure[IO].map(ObjectUrl.apply)
    }

  def objectsByPathServerEndpoint(authorizationService: AuthorizationService, s3Service: S3Service, resourceGlobalName: String, defaultBucket: String): ServerEndpoint[Any, IO] =
    objectsByPaths(authorizationService, resourceGlobalName).serverLogicSuccess { user =>
      files =>
        files.split("\n")
          .toList
          .traverse(file => s3Service.presignedUrl(defaultBucket, file).pure[IO].map(u => file -> u)).map(_.toMap)
    }

  def all(config: Config, authorizationService: AuthorizationService, s3Service: S3Service): Seq[ServerEndpoint[Any, IO]] = {
    val s: Option[List[ServerEndpoint[Any, IO]]] = for {
      b <- config.s3Config.defaultBucket
      r <- config.auth.resourcesGlobalName
      servers = List(
        objectByPathServer(authorizationService, s3Service, r, b),
        objectsByPathServerEndpoint(authorizationService, s3Service, r, b)
      )
    } yield servers
    s.getOrElse(Nil)
  }



