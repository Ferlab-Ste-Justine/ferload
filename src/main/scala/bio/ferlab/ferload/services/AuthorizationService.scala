package bio.ferlab.ferload.services

import bio.ferlab.ferload.AuthConfig
import bio.ferlab.ferload.model.*
import cats.effect.IO
import com.github.benmanes.caffeine.cache.{AsyncCacheLoader, AsyncLoadingCache, Caffeine, Expiry}
import io.circe.Error
import io.circe.generic.auto.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model
import sttp.model.MediaType.ApplicationXWwwFormUrlencoded
import sttp.model.StatusCode

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

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

  private def introspectPartyToken(partyToken: String): IO[IntrospectResponse] = {

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
      val value: Set[Permissions] = permissionToken.permissions.map(_.toSet).getOrElse(Set.empty)
      User(partyToken, value)
    }

    r.map {
        case User(_, permissions) if containAllPermissions(resources, permissions) => Right(User(token, permissions))
        case _ => Left((StatusCode.Forbidden, ErrorResponse("Forbidden", 403)))
      }
      .recover {
        case HttpError(_, statusCode) if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(statusCode) => Left((statusCode, ErrorResponse("Unauthorized", statusCode.code))).withRight[User]
        case e: HttpError[String] if e.statusCode == StatusCode.BadRequest && e.body.contains("invalid_resource") => Left((StatusCode.NotFound, ErrorResponse("Not Found", 404))).withRight[User]
      }

  }

  def getResourceById(id: String): IO[Resource] = {
    for {
      token <- clientToken()
      resource <- fetchResourceById(id, token.access_token)
    } yield resource

  }

  private def fetchResourceById(id: String, token: String): IO[Resource] = {

    val auth: IO[Response[Either[ResponseException[String, Error], Resource]]] = basicRequest.get(uri"$baseUri/authz/protection/resource_set/$id")
      .auth.bearer(token)
      .response(asJson[Resource])
      .send(backend)
    auth.flatMap(r => IO.fromEither(r.body))

  }

  def existResource(id: String, token: String): IO[StatusCode] = {

    val auth: IO[Response[Either[String, String]]] = basicRequest.get(uri"$baseUri/authz/protection/resource_set/$id")
      .auth.bearer(token)
      .send(backend)
    auth.map(r => r.code)

  }

  private def requestClientToken(): IO[PartyToken] = {
    val body: Seq[(String, String)] = Seq(
      "client_id" -> authConfig.clientId,
      "client_secret" -> authConfig.clientSecret,
      "grant_type" -> "client_credentials"
    )
    val auth: IO[Response[Either[ResponseException[String, Error], PartyToken]]] = basicRequest.post(uri"$baseUri/protocol/openid-connect/token")
      .contentType(ApplicationXWwwFormUrlencoded)
      .body(body, "utf-8")
      .response(asJson[PartyToken])
      .send(backend)
    auth.flatMap(r => IO.fromEither(r.body))
  }

  private val cacheLoader: AsyncCacheLoader[String, PartyToken] = (key: String, executor: Executor) => {
    import cats.effect.unsafe.implicits.global
    requestClientToken().evalOn(ExecutionContext.fromExecutor(executor)).unsafeToCompletableFuture()
  }

  private val expiry = new Expiry[String, PartyToken]() {
    override def expireAfterCreate(key: String, value: PartyToken, currentTime: Long): Long = {
      val d = value.expires_in * 1E9 - 5 * 1E9
      d.toLong
    }

    override def expireAfterUpdate(key: String, value: PartyToken, currentTime: Long, currentDuration: Long): Long = {
      currentDuration
    }

    override def expireAfterRead(key: String, value: PartyToken, currentTime: Long, currentDuration: Long): Long = {
      currentDuration
    }

  }
  val cache: AsyncLoadingCache[String, PartyToken] = Caffeine.newBuilder()
    .expireAfter(expiry)
    .buildAsync(cacheLoader)

  def clientToken(): IO[PartyToken] = IO.fromCompletableFuture(IO(cache.get("client_token")))

  private def containAllPermissions(resources: Seq[String], permissions: Set[Permissions]): Boolean = {
    resources.forall(r => {
      val resourceInPermissions = permissions.flatMap(_.rsname)
      resourceInPermissions.contains(r)
    })
  }
}





