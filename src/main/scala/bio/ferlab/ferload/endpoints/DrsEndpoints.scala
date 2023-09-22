package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.{Config, DrsConfig}
import bio.ferlab.ferload.model.ErrorResponse
import bio.ferlab.ferload.model.drs.*
import bio.ferlab.ferload.services.{AuthorizationService, S3Service}
import cats.effect.IO
import cats.implicits.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

object DrsEndpoints:
  val baseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = endpoint
    .prependSecurityIn("ga4gh")
    .prependSecurityIn("drs")
    .prependSecurityIn("v1")

  private val service = baseEndpoint.get
    .in("service-info")
    .out(jsonBody[Service])

  private val objectEnpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = baseEndpoint
    .securityIn("objects")

  private def serviceServer(drsConfig: DrsConfig) = service.serverLogicSuccess(_ =>
    Service(
      id = drsConfig.id,
      name = drsConfig.name,
      organization = ServiceOrganization(drsConfig.organizationName, drsConfig.organizationUrl),
      `type` = ServiceType("org.ga4gh", "drs", "1.3.0"),
      version = drsConfig.version,
      description = drsConfig.description,
      contactUrl = drsConfig.contactUrl,
      documentationUrl = drsConfig.documentationUrl,
      createdAt = None,
      updatedAt = None,
      environment = drsConfig.environment
    ).pure[IO]
  )


  private def objectInfo: Endpoint[Unit, String, (StatusCode, ErrorResponse), Authorizations, Any] =
    objectEnpoint.in(path[String].name("object_id"))
      .options
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .out(jsonBody[Authorizations])

  private def getObject(authorizationService: AuthorizationService) =
    objectEnpoint
      .securityIn(auth.bearer[String]())
      .securityIn(path[String].name("object_id"))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .serverSecurityLogic((token, objectId) => authorizationService.authLogic(token, Seq(objectId)))
      .get
      .out(jsonBody[DrsObject])

  private def objectInfoServer(config: Config, authorizationService: AuthorizationService) = objectInfo.serverLogic { objectId =>
    for {
      token <- authorizationService.clientToken()
      _ <- IO.println(token.expires_in)
      _ <- IO.println(token.access_token)
      existResource <- authorizationService.existResource(objectId, token.access_token)

    } yield existResource match {
      case StatusCode.Ok => Right(Authorizations(Some(List("BearerAuth")), None, Some(List(s"${config.auth.authUrl}/realms/${config.auth.realm}"))))
      case StatusCode.NotFound => Left((StatusCode.NotFound, ErrorResponse(s"Object $objectId not found", 404)))
      case e => throw new IllegalStateException(s"Unexpected status code: ${e.code}")
    }
  }

  private def getObjectServer(config: Config, authorizationService: AuthorizationService, s3Service: S3Service) = getObject(authorizationService).serverLogicSuccess { user =>
    _ =>
      for {
        resource <- authorizationService.getResourceById(user.permissions.head.resource_id)
        bucketAndPath <- IO.fromTry(S3Service.parseS3Urls(resource.uris))
        (bucket, path) = bucketAndPath
        url = s3Service.presignedUrl(bucket, path)
      } yield DrsObject.build(resource, url, config.http.host)
  }


  def all(config: Config, authorizationService: AuthorizationService, s3Service: S3Service): Seq[ServerEndpoint[Any, IO]] = Seq(serviceServer(config.drsConfig), objectInfoServer(config, authorizationService), getObjectServer(config, authorizationService, s3Service))