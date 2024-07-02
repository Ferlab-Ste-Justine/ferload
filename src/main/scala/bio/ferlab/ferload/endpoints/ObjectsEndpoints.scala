package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.Config
import bio.ferlab.ferload.endpoints.SecuredEndpoint.baseEndpoint
import bio.ferlab.ferload.model.{ErrorResponse, ObjectUrl, ReadResource, User}
import bio.ferlab.ferload.services.{AuthorizationService, ResourceService, S3Service}
import cats.effect.IO
import cats.implicits.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import io.circe.generic.auto.*

object ObjectsEndpoints:


  object ById:

    private val byIdEndpoint = baseEndpoint.securityIn("objects")

    private def singleObject(authorizationService: AuthorizationService): PartialServerEndpoint[(String, String), (User, Option[String]), Unit, (StatusCode, ErrorResponse), ObjectUrl, Any, IO] = byIdEndpoint
      .get
      .securityIn(path[String].name("object_id"))
      .serverSecurityLogic((token, objectId) => authorizationService.authLogic(token, Seq(objectId)))
      .description("Retrieve an object by its id and return an url to download it")
      .out(jsonBody[ObjectUrl])

    private def listObjects(authorizationService: AuthorizationService): PartialServerEndpoint[(String, String), (User, Option[String]), Unit, (StatusCode, ErrorResponse), Map[String, String], Any, IO] = byIdEndpoint
      .post
      .securityIn("list")
      .securityIn(stringBody.description("List of ids of objects to retrieve").example("FI1\nFI2"))
      .serverSecurityLogic((token, objects) => authorizationService.authLogic(token, objects.split("\n")))
      .description("Retrieve an object by its id and return an url to download it")
      .out(jsonBody[Map[String, String]]
        .description("List of files URLs by object id")
        .example(Map("FI1" -> "https://file1.vcf", "FI2" -> "https://file2.vcf")))


    def singleObjectServer(authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): ServerEndpoint[Any, IO] =
      singleObject(authorizationService).serverLogicSuccess { (user, _) =>
        _ =>
          for {
            resource <- resourceService.getResourceById(user.permissions.head.rsid)
            bucketAndPath <- IO.fromTry(S3Service.parseS3Urls(resource.uris))
            (bucket, path) = bucketAndPath
            url = s3Service.presignedUrl(bucket, path)
          } yield ObjectUrl(url)

      }


    def listObjectsServer(authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): ServerEndpoint[Any, IO] =
      listObjects(authorizationService).serverLogicSuccess { (user, _) =>
        _ =>
          val resourcesIO: IO[List[ReadResource]] = user.permissions.toList.traverse(p => resourceService.getResourceById(p.rsid))
          resourcesIO.map { resources =>
            val urls: Seq[(String, (String, String))] = resources.flatMap(r => S3Service.parseS3Urls(r.uris).toOption.map(r.name -> _))
            val m: Map[String, String] = urls.map { case (name, (bucket, path)) => name -> s3Service.presignedUrl(bucket, path) }.toMap
            m
          }


      }

    def all(authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): Seq[ServerEndpoint[Any, IO]] = List(
      singleObjectServer(authorizationService, resourceService, s3Service),
      listObjectsServer(authorizationService, resourceService, s3Service)
    )

  object ByPath:
    private def byPathEndpoint(authorizationService: AuthorizationService, resourceGlobalName: String): PartialServerEndpoint[String, (User, Option[String]), Unit, (StatusCode, ErrorResponse), Unit, Any, IO] =
      baseEndpoint
        .securityIn("objects")
        .securityIn("bypath")
        .serverSecurityLogic(token => authorizationService.authLogic(token, Seq(resourceGlobalName)))

    private def singleObject(authorizationService: AuthorizationService, resourceGlobalName: String): PartialServerEndpoint[String, (User, Option[String]), String, (StatusCode, ErrorResponse), ObjectUrl, Any, IO] =
      byPathEndpoint(authorizationService, resourceGlobalName)
        .get
        .description("Retrieve an object by its path and return an url to download it")
        .in(query[String]("path").description("Path of the object to retrieve").example("dir1/file1.vcf"))
        .out(jsonBody[ObjectUrl])

    def singleObjectServer(authorizationService: AuthorizationService, s3Service: S3Service, resourceGlobalName: String, defaultBucket: String): ServerEndpoint[Any, IO] =
      singleObject(authorizationService, resourceGlobalName).serverLogicSuccess { user =>
        file => s3Service.presignedUrl(defaultBucket, file).pure[IO].map(ObjectUrl.apply)
      }

    private def listObjects(authorizationService: AuthorizationService, resourceGlobalName: String): PartialServerEndpoint[String, (User, Option[String]), String, (StatusCode, ErrorResponse), Map[String, String], Any, IO] = byPathEndpoint(authorizationService, resourceGlobalName)
      .description("Retrieve a list of objects by their path and return a list of download URLs for each object")
      .post
      .in("list")
      .in(stringBody.description("List of URLs of objects to retrieve").example("file1.vcf\nfile2.vcf"))
      .out(jsonBody[Map[String, String]]
        .description("List of files URLs by file name")
        .example(Map("dir1/file1.vcf" -> "https://file1.vcf", "dir1/file2.vcf" -> "https://file2.vcf"))
      )

    def listObjectsServer(authorizationService: AuthorizationService, s3Service: S3Service, resourceGlobalName: String, defaultBucket: String): ServerEndpoint[Any, IO] = listObjects(authorizationService, resourceGlobalName).serverLogicSuccess { user =>
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
          singleObjectServer(authorizationService, s3Service, r, b),
          listObjectsServer(authorizationService, s3Service, r, b)
        )
      } yield servers
      s.getOrElse(Nil)

    }

  def all(config: Config, authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): Seq[ServerEndpoint[Any, IO]] =
    ByPath.all(config, authorizationService, s3Service) ++ ById.all(authorizationService, resourceService, s3Service)

