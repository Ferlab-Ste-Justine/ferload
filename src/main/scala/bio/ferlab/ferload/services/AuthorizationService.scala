package bio.ferlab.ferload.services

import bio.ferlab.ferload.{AuthConfig, FerloadClientConfig}
import bio.ferlab.ferload.endpoints.PermissionsEndpoints.InputPermissions
import bio.ferlab.ferload.endpoints.RawPermissions
import bio.ferlab.ferload.model.*
import cats.effect.IO
import io.circe.{Error, Json}
import io.circe.generic.auto.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model
import sttp.model.MediaType.ApplicationXWwwFormUrlencoded
import sttp.model.StatusCode

import java.lang

/**
 * Service used to authorize a user to access resources
 *
 * @param authConfig configuration of open id server
 * @param backend    sttp backend used to query the open id server
 */
class AuthorizationService(authConfig: AuthConfig, backend: SttpBackend[IO, Fs2Streams[IO]]) {
  
  /**
   * Exchange a token for a Request Party Token (RPT) that can be used to access resources passed in the resources parameter.
   *
   * @param token     the token to exchange
   * @param resources the resources to access
   * @return the RPT
   */
  protected[services] def requestPartyToken(token: String, resources: Seq[String]): IO[String] = {
    val body: Seq[(String, String)] = Seq(
      "grant_type" -> "urn:ietf:params:oauth:grant-type:uma-ticket",
      "audience" -> authConfig.clientId,
    ) ++ resources.map(r => "permission" -> r)

    val auth: IO[Response[Either[ResponseException[String, Error], PartyToken]]] = basicRequest.post(uri"${authConfig.baseUri}/protocol/openid-connect/token")
      .auth.bearer(token)
      .contentType(ApplicationXWwwFormUrlencoded)
      .body(body, "utf-8")
      .response(asJson[PartyToken])
      .send(backend)
    auth.flatMap(r => IO.fromEither(r.body).map(_.access_token))
  }

  /**
   * Exchange a token for a Request Party Token (RPT) to return the list of user permissions.
   *
   * @param token     the token to exchange
   * @return list of user permissions
   */
  protected[services] def requestUserPermissions(token: String): IO[Seq[RawPermissions]] = {
    val body: Seq[(String, String)] = Seq(
      "grant_type" -> "urn:ietf:params:oauth:grant-type:uma-ticket",
      "audience" -> authConfig.clientId,
      "response_mode" -> "permissions",
    )

    val auth: IO[Response[Either[ResponseException[String, Error], Seq[RawPermissions]]]] = basicRequest.post(uri"${authConfig.baseUri}/protocol/openid-connect/token")
      .auth.bearer(token)
      .contentType(ApplicationXWwwFormUrlencoded)
      .body(body, "utf-8")
      .response(asJson[Seq[RawPermissions]])
      .send(backend)

    auth.flatMap(r => IO.fromEither(r.body))
  }

  /**
   * Introspect a token to get contents.
   *
   * @param token the token to introspect
   * @return the response from the introspection endpoint
   */
  protected[services] def introspectToken(token: String): IO[IntrospectResponse] = {

    val introspect = basicRequest.post(uri"${authConfig.baseUri}/protocol/openid-connect/token/introspect")
      .contentType(ApplicationXWwwFormUrlencoded)
      .body(
        "token" -> token,
        "client_id" -> authConfig.clientId,
        "client_secret" -> authConfig.clientSecret)
      .response(asJson[IntrospectResponse])
      .send(backend)
    introspect.flatMap(r => {
      IO.fromEither(r.body)
    }

      )

  }

  /**
   * Validate a token and return the user with permissions if the token is valid and if user have access to the resources. Otherwise, return an error.
   *
   * @param token     the token to validate
   * @param resources the resources to access
   * @return the user with permissions if the token is valid and if user have access to the resources. Otherwise, return errors (Unauthorized, Forbidden, NotFound).
   */
  def authLogic(token: String, resources: Seq[String], method: String, accessId: Option[String] = None): IO[Either[(StatusCode, ErrorResponse), (User, Option[String])]] = {
    val r: IO[(User, Option[String])] = for {
      accessToken <- introspectToken(token)
      partyToken <- requestPartyToken(token, resources)
      permissionToken <- introspectToken(partyToken)
    } yield {


      // For device method we only authorize tokens from a specific client
      if(method == FerloadClientConfig.DEVICE) {
        val isAuthorizedClientAccessToken = accessToken.azp.exists(_.equalsIgnoreCase(authConfig.audience.get))
        if (!isAuthorizedClientAccessToken) {
          throw HttpError(s"Unauthorized client: ${accessToken.azp.getOrElse("Nothing")}", StatusCode.Forbidden)
        }
      }

      val value: Set[Permissions] = permissionToken.authorization.map(_.permissions.toSet).getOrElse(Set.empty)
      (User(partyToken, value), accessId)
    }

    r.map {
        case (User(_, permissions), accessId) if containAllPermissions(resources, permissions) => Right((User(token, permissions), accessId))
        case (User(_, permissions), _) => Left((StatusCode.Forbidden, ErrorResponse(resources.filterNot(permissions.map(_.rsid).contains).mkString("[",",","]"), 403)))
      }
      .recover {
        case HttpError(_, statusCode) if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(statusCode) => Left((statusCode, ErrorResponse("Unauthorized", statusCode.code))).withRight[(User, Option[String])]
        case e: HttpError[String] if e.statusCode == StatusCode.BadRequest && e.body.contains("invalid_resource") => Left((StatusCode.NotFound, ErrorResponse("Not Found", 404))).withRight[(User, Option[String])]
      }

  }

  /**
   * Fetch only resources user has access from a provided input list of resources ids. Return a list resources id the user is authorized. Otherwise, return an error.
   *
   * @param token     the token to validate
   * @param resources the resources to access
   * @return ta list resources id the user is authorized. Otherwise, return errors (Unauthorized, Forbidden, NotFound).
   */
  def authLogicAuthorizationForUser(token: String, resources: Json): IO[Either[(StatusCode, ErrorResponse), User]] = {
    val parsedResources = resources.as[InputPermissions]

    val fileIds = parsedResources match {
      case Left(parsingError) =>
        throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => json.file_ids
    }

    val r: IO[User] = for {
      partyToken <- requestPartyToken(token, fileIds)
      permissionToken <- introspectToken(partyToken)
    } yield {
      val value: Set[Permissions] = permissionToken.authorization.map(au => au.permissions.toSet).getOrElse(Set.empty)
      User(partyToken, value)
    }

    r.map {
        case User(_, permissions) => Right(User(token, permissions))
      }
      .recover {
        case HttpError(_, statusCode) if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(statusCode) => Left((statusCode, ErrorResponse("Unauthorized", statusCode.code))).withRight[User]
        case e: HttpError[String] if e.statusCode == StatusCode.BadRequest && e.body.contains("invalid_resource") => Left((StatusCode.NotFound, ErrorResponse("Not Found", 404))).withRight[User]
      }

  }

  /**
   * Fetch all resources user has access. Return a list resources id the user is authorized. Otherwise, return an error.
   *
   * @param token     the token to validate
   * @return ta list resources id the user is authorized. Otherwise, return errors (Unauthorized, Forbidden, NotFound).
   */
  def authLogicAuthorizationForUser(token: String): IO[Either[(StatusCode, ErrorResponse), Seq[String]]] = {
    val r: IO[Seq[String]] = for {
      userPermissions <- requestUserPermissions(token)
    } yield {
      userPermissions.map(_.rsid)
    }

    r.map {
        case resource: Seq[String] => Right(resource)
      }
      .recover {
        case HttpError(_, statusCode) if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(statusCode) => Left((statusCode, ErrorResponse("Unauthorized", statusCode.code))).withRight[Seq[String]]
        case e: HttpError[String] if e.statusCode == StatusCode.BadRequest && e.body.contains("invalid_resource") => Left((StatusCode.NotFound, ErrorResponse("Not Found", 404))).withRight[Seq[String]]
      }

  }

  /**
   * Verifies if the permissions contains all the resources.
   *
   * @param resources   the resources to access
   * @param permissions the permissions to check
   * @return true if the permissions contains all the resources, false otherwise
   */
  private def containAllPermissions(resources: Seq[String], permissions: Set[Permissions]): Boolean = {
    resources.forall(r => {
      val resourceInPermissions = permissions.flatMap(_.rsname)
      resourceInPermissions.contains(r)
    })
  }
}





