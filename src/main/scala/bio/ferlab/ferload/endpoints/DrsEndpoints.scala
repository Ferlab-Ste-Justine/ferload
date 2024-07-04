package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.model.ErrorResponse
import bio.ferlab.ferload.model.drs.*
import bio.ferlab.ferload.model.drs.CreateDrsObject.toResource
import bio.ferlab.ferload.services.{AuthorizationService, ResourceService, S3Service}
import bio.ferlab.ferload.{Config, DrsConfig}
import cats.effect.IO
import cats.implicits.*
import io.circe.generic.auto.*
import sttp.client3.HttpError
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

object DrsEndpoints:
  val baseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = endpoint
    .prependSecurityIn("v1")
    .prependSecurityIn("drs")
    .prependSecurityIn("ga4gh")

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


  private def getAccessMethod(authorizationService: AuthorizationService) =
    objectEnpoint
      .securityIn(auth.bearer[String]())
      .securityIn(path[String].name("object_id"))
      .securityIn("access" / path[String].name("access_id"))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .serverSecurityLogic((token, objectId, accessId) => authorizationService.authLogic(token, Seq(objectId), Some(accessId)))
      .get
      .out(jsonBody[AccessURL])

  private val createObject: Endpoint[Unit, (String, CreateDrsObject), (StatusCode, ErrorResponse), StatusCode, Any] =
    baseEndpoint
      .in("object")
      .in(auth.bearer[String]())
      .in(jsonBody[CreateDrsObject])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .out(statusCode)
      .post

  private def objectInfoServer(config: Config, resourceService: ResourceService) = objectInfo.serverLogic { objectId =>
    for {
      existResource <- resourceService.existResource(objectId)
    } yield existResource match {
      case StatusCode.Ok => Right(Authorizations(Some(List("BearerAuth")), None, Some(List(s"${config.auth.authUrl}/realms/${config.auth.realm}"))))
      case StatusCode.NotFound => Left((StatusCode.NotFound, ErrorResponse(s"Object $objectId not found", 404)))
      case e => throw new IllegalStateException(s"Unexpected status code: ${e.code}")
    }
  }

  private def getObjectServer(config: Config, authorizationService: AuthorizationService, resourceService: ResourceService) = getObject(authorizationService).serverLogicSuccess { (user, _) =>
    _ =>
      for {
        resource <- resourceService.getResourceById(user.permissions.head.rsid)
      // For now, we only have a unique accessId (all resources are in CEPH S3)
      } yield DrsObject.build(resource, config.drsConfig.accessId, config.drsConfig.selfHost)
  }


  private def getAccessMethodServer(config: Config, authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service) = getAccessMethod(authorizationService).serverLogicSuccess { (user, accessId) =>
    _ =>
      //fetch according to accessId, it is unique for now
      if(accessId.isEmpty || config.drsConfig.accessId != accessId.get){
        throw HttpError(s"Access Id not found: $accessId", StatusCode.NotFound)
      }

      for {
        resource <- resourceService.getResourceById(user.permissions.head.rsid)
        bucketAndPath <- IO.fromTry(S3Service.parseS3Urls(resource.uris))
        (bucket, path) = bucketAndPath
        url = s3Service.presignedUrl(bucket, path)
      } yield
        AccessURL.build(url)
  }

  private def createObjectServer(config: Config, resourceService: ResourceService) = createObject.serverLogicSuccess { (token, createDrsObject) =>
    val existResources = resourceService.existResource(createDrsObject.id)
    existResources.flatMap {
      case StatusCode.Ok => resourceService.updateResource(token, toResource(createDrsObject))
      case StatusCode.NotFound => resourceService.createResource(token, toResource(createDrsObject))
      case e => IO.raiseError(new IllegalStateException(s"Unexpected status code: $e"))
    }


  }

  def all(config: Config, authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): Seq[ServerEndpoint[Any, IO]] = Seq(
    serviceServer(config.drsConfig),
    objectInfoServer(config, resourceService),
    getObjectServer(config, authorizationService, resourceService),
    getAccessMethodServer(config, authorizationService, resourceService, s3Service),
    createObjectServer(config, resourceService)
  )