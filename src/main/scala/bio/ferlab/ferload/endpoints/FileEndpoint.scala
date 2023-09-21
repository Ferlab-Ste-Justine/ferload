package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.Config
import bio.ferlab.ferload.services.AuthorizationService.{ErrorResponse, User}
import bio.ferlab.ferload.services.{AuthorizationService, S3Service}
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import cats.implicits._

object FileEndpoint:

  case class FileUrl(url: String)


  private def securedGlobalEndpoint(authorizationService: AuthorizationService): PartialServerEndpoint[String, User, Unit, (StatusCode, ErrorResponse), Unit, Any, IO] =
    endpoint
      .securityIn(auth.bearer[String]())
      .errorOut(statusCode)
      .errorOut(jsonBody[ErrorResponse])
      .serverSecurityLogic(token => authorizationService.globalAuthLogic(token))

  private def fileEndpoint(authorizationService: AuthorizationService): PartialServerEndpoint[String, User, List[String], (StatusCode, ErrorResponse), FileUrl, Any, IO] = securedGlobalEndpoint(authorizationService)
    .get
    .description("Retrieve a file by its URL")
    .in(paths.description("URL of the file to retrieve"))
    .out(jsonBody[FileUrl])

  private def filesEndpoint(authorizationService: AuthorizationService): PartialServerEndpoint[String, User, String, (StatusCode, ErrorResponse), Map[String, String], Any, IO] = securedGlobalEndpoint(authorizationService)
    .description("Retrieve a list of files by URLs")
    .post
    .in("downloadLinks")
    .in(stringBody.description("List of URLs of the files to retrieve").example("file1.vcf\nfile2.vcf"))
    .out(jsonBody[Map[String, String]]
      .description("List of files URLs by file name")
      .example(Map("file1.vcf" -> "https://file1.vcf", "file2.vcf" -> "https://file2.vcf"))
    )

  def fileServerEndpoint(config: Config, authorizationService: AuthorizationService, s3Service: S3Service): ServerEndpoint[Any, IO] = fileEndpoint(authorizationService).serverLogicSuccess { user =>
    file => s3Service.presignedUrlDefaultBucket(file.mkString("/")).map(FileUrl.apply)
  }

  def filesServerEndpoint(config: Config, authorizationService: AuthorizationService, s3Service: S3Service): ServerEndpoint[Any, IO] = filesEndpoint(authorizationService).serverLogicSuccess { user =>
    files =>
      files.split("\n")
        .toList
        .traverse(file => s3Service.presignedUrlDefaultBucket(file).map(u => file -> u)).map(_.toMap)
  }


