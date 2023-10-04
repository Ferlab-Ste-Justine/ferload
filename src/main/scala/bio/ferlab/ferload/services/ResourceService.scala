package bio.ferlab.ferload.services

import bio.ferlab.ferload.AuthConfig
import bio.ferlab.ferload.model.{PartyToken, Permissions, ReadResource, WriteResource}
import cats.effect.IO
import com.github.benmanes.caffeine.cache.{AsyncCacheLoader, AsyncLoadingCache, Caffeine, Expiry}
import io.circe.Error
import io.circe.generic.auto.*
import sttp.client3.circe.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.circe.asJson
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.MediaType.ApplicationXWwwFormUrlencoded
import sttp.model.StatusCode
import sttp.client3._
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

class ResourceService(authConfig: AuthConfig, backend: SttpBackend[IO, Fs2Streams[IO]]) {

  /**
   * Get a resource by id
   *
   * @param id id of the resource to get
   * @return the resource if exist or an error
   */
  def getResourceById(id: String): IO[ReadResource] = {
    for {
      token <- clientToken()
      resource <- fetchResourceById(id, token.access_token)
    } yield resource

  }

  private def resourceRequest(token: String) = {
    basicRequest
      .auth.bearer(token)
  }

  /**
   * Fetch a resource by id
   *
   * @param id    id of the resource to get
   * @param token token to use to fetch the resource
   * @return the resource if exist or an error
   */
  private def fetchResourceById(id: String, token: String): IO[ReadResource] = {
    val auth: IO[Response[Either[ResponseException[String, Error], ReadResource]]] = resourceRequest(token)
      .get(uri"${authConfig.baseUri}/authz/protection/resource_set/$id")
      .response(asJson[ReadResource])
      .send(backend)
    auth.flatMap(r => IO.fromEither(r.body))
  }

  def createResource(token: String, resource: WriteResource): IO[StatusCode] = {
    val resp = resourceRequest(token)
      .body(resource)
      .post(uri"${authConfig.baseUri}/authz/protection/resource_set")
      .response(asStringAlways)
      .send(backend)
      .flatMap {
        case r if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(r.code)  || r.isSuccess => IO.pure(r.code)
        case r => IO.raiseError(HttpError(r.body, r.code))
      }
    resp
  }

  def updateResource(token: String, resource: WriteResource): IO[StatusCode] = {
    val resp = resourceRequest(token)
      .body(resource)
      .put(uri"${authConfig.baseUri}/authz/protection/resource_set/${resource.id}")
      .response(asStringAlways)
      .send(backend)
      .flatMap {
        case r if Seq(StatusCode.Unauthorized, StatusCode.Forbidden).contains(r.code) || r.isSuccess => IO.pure(r.code)
        case r => IO.raiseError(HttpError(r.body, r.code))
      }
    resp

  }

  /**
   * Check if a resource exist
   *
   * @param id id of the resource to check
   * @return 200 if the resource exist, 404 if not
   */
  def existResource(id: String): IO[StatusCode] = {
    for {
      token <- clientToken()
      resp <- resourceRequest(token.access_token)
        .get(uri"${authConfig.baseUri}/authz/protection/resource_set/$id")
        .send(backend)
    } yield resp.code
  }

  /**
   * Fetch a client token based on authConfig
   *
   * @return the token
   */
  private def requestClientToken(): IO[PartyToken] = {
    val body: Seq[(String, String)] = Seq(
      "client_id" -> authConfig.clientId,
      "client_secret" -> authConfig.clientSecret,
      "grant_type" -> "client_credentials"
    )
    val auth: IO[Response[Either[ResponseException[String, Error], PartyToken]]] =
      basicRequest.post(uri"${authConfig.baseUri}/protocol/openid-connect/token")
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

  protected[services] def clientToken(): IO[PartyToken] = IO.fromCompletableFuture(IO(cache.get("client_token")))


}
