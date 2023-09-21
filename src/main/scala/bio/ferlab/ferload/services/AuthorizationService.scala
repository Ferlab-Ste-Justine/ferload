package bio.ferlab.ferload.services

import bio.ferlab.ferload.AuthConfig
import bio.ferlab.ferload.services.AuthorizationService.{ErrorResponse, IntrospectResponse, PartyToken, User}
import cats.effect.{IO, Resource}
import io.circe.Error
import io.circe.generic.auto.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model
import sttp.model.MediaType.ApplicationXWwwFormUrlencoded
import sttp.model.StatusCode

class AuthorizationService(authConfig: AuthConfig, backend: SttpBackend[IO, Fs2Streams[IO]]) {

  private val baseUri = s"${authConfig.authUrl}/realms/${authConfig.realm}"

  def requestPartyToken(token: String, resources: Seq[String]): IO[String] = {

    val body: Seq[(String, String)] = Seq(
      "grant_type" -> "urn:ietf:params:oauth:grant-type:uma-ticket",
      "audience" -> authConfig.clientId,
    ) ++ resources.map(r => "permission" -> r)

    val auth: IO[Response[Either[ResponseException[String, Error], PartyToken]]] = basicRequest.post(uri"$baseUri/protocol/openid-connect/token")
      .auth.bearer(token)
      .contentType(ApplicationXWwwFormUrlencoded)
      .body(body, "utf-8")
      .response(asJson[PartyToken])
      .send(backend)

    auth.flatMap(r => IO.fromEither(r.body).map(_.access_token))
  }

  def introspectPartyToken(partyToken: String): IO[IntrospectResponse] = {

    val introspect = basicRequest.post(uri"$baseUri/protocol/openid-connect/token/introspect")
      .contentType(ApplicationXWwwFormUrlencoded)
      .body("token_type_hint" -> "requesting_party_token",
        "token" -> partyToken,
        "client_id" -> authConfig.clientId,
        "client_secret" -> authConfig.clientSecret)
      .response(asJson[IntrospectResponse])
      .send(backend)
    introspect.flatMap(r => IO.fromEither(r.body))

  }

  def authLogic(token: String, resources: Seq[String]): IO[Either[(StatusCode, ErrorResponse), User]] = {
    val data = requestPartyToken(token, resources)
    val r: IO[User] = for {
      partyToken <- requestPartyToken(token, resources)
      permissionToken <- introspectPartyToken(partyToken)
    } yield {
      val value: Set[AuthorizationService.Permissions] = permissionToken.permissions.map(_.toSet).getOrElse(Set.empty)
      User(partyToken, value)
    }

    r.map {
        case User(_, permissions) if containAllPermissions(resources, permissions) => Right(User(token, permissions))
        case _ => Left((StatusCode.Forbidden, ErrorResponse("Forbidden")))
      }
      .recover { case HttpError(_, statusCode) if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(statusCode) => Left((statusCode, ErrorResponse("Unauthorized"))).withRight[User] }

  }

  private def containAllPermissions(resources: Seq[String], permissions: Set[AuthorizationService.Permissions]): Boolean = {
    resources.forall(r => {
      val resourceInPermissions = permissions.flatMap(_.rsname)
      resourceInPermissions.contains(r)
    })
  }

  def globalAuthLogic(token: String): IO[Either[(StatusCode, ErrorResponse), User]] = {
    authConfig.resourcesGlobalName.map {
      resourceGlobalName => authLogic(token, Seq(resourceGlobalName))
    }.getOrElse(IO.pure(Left((StatusCode.Forbidden, ErrorResponse("Forbidden")))))
  }
}

object AuthorizationService:
  case class PartyToken(access_token: String, expires_in: Int, refresh_expires_in: Int, refresh_token: String, token_type: String)

  case class IntrospectResponse(active: Boolean, exp: Option[Int], iat: Option[Int], aud: Option[String], nbf: Option[Int], permissions: Option[Seq[Permissions]])

  case class Permissions(resource_id: String, rsname: Option[String], resource_scopes: Seq[String])

  case class User(token: String, permissions: Set[Permissions])

  case class ErrorResponse(message: String)


