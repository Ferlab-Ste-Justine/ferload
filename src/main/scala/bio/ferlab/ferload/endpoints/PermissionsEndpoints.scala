package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.endpoints.SecuredEndpoint.baseEndpoint
import bio.ferlab.ferload.model.{ErrorResponse, User}
import bio.ferlab.ferload.services.AuthorizationService
import cats.effect.IO
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.generic.auto._
import io.circe._, io.circe.parser._

case class RawPermissions(rsid: String)

object PermissionsEndpoints:

  case class InputPermissions(file_ids: Seq[String])

  stringJsonBody.schema(implicitly[Schema[InputPermissions]].as[String])


  object ById:

    private val byIdEndpoint = baseEndpoint.prependSecurityIn("permissions")

    private def allUserPermissions(authorizationService: AuthorizationService): PartialServerEndpoint[String, Seq[String], Unit, (StatusCode, ErrorResponse), List[String], Any, IO] = byIdEndpoint
      .post
      .securityIn("for-user")
      .serverSecurityLogic(token => authorizationService.authLogicAuthorizationForUser(token))
      .description("Retrieve all permissions for target user")
      .out(jsonBody[List[String]]
        .description("List of object id authorized for user")
        .example(List("FI1", "FI2")))

    private def listPermissions(authorizationService: AuthorizationService): PartialServerEndpoint[(String, io.circe.Json), User, Unit, (StatusCode, ErrorResponse), List[String], Any, IO] = byIdEndpoint
      .post
      .securityIn("by-list")
      .securityIn(jsonBody.description("List of ids of objects to retrieve").example(parse("""{"file_ids":["FI1","FI2"]}""".stripMargin).getOrElse(io.circe.Json.Null)))
      .serverSecurityLogic((token, objects) => authorizationService.authLogicAuthorizationForUser(token, objects))
      .description("Return list of object Id the user can download from a provided input list")
      .out(jsonBody[List[String]]
        .description("List of object id authorized for user")
        .example(List("FI1", "FI2")))

    private def allUserPermissionsServer(authorizationService: AuthorizationService): ServerEndpoint[Any, IO] =
      allUserPermissions(authorizationService).serverLogicSuccess { user =>
        _ =>
          IO(user.toList)

      }


    private def listPermissionsServer(authorizationService: AuthorizationService): ServerEndpoint[Any, IO] =
      listPermissions(authorizationService).serverLogicSuccess { user =>
        _ =>
          IO(user.permissions.map(_.rsid).toList)
      }

    def all(authorizationService: AuthorizationService): Seq[ServerEndpoint[Any, IO]] = List(
      listPermissionsServer(authorizationService),
      allUserPermissionsServer(authorizationService),
    )

