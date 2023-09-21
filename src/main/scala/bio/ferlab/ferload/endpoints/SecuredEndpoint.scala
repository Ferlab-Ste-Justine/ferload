package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.services.AuthorizationService
import bio.ferlab.ferload.services.AuthorizationService.Permissions
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.uri
import org.http4s.{Method, Request}
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.*
import sttp.tapir.generic.auto.*
import io.circe.generic.auto.*
import sttp.client3.HttpError
import sttp.model.StatusCode

import scala.util.Right

object SecuredEndpoint:

  

  case class KeyData(kid: String, n: String, e: String)

//  def authLogic(authorizationService: AuthorizationService)(token: String, resources:String): IO[Either[(StatusCode, ErrorResponse), User]] = {
//    val data = authorizationService.requestPartyToken(token, Seq("FI100", "FI1"))
//    val r: IO[User] = for {
//      partyToken <- authorizationService.requestPartyToken(token, Seq("FI100", "FI1"))
//      permissionToken <- authorizationService.introspectPartyToken(partyToken)
//    } yield User(partyToken)
//
//    val value: IO[Either[(StatusCode, ErrorResponse), User]] = r
//      .map(Right(_).withLeft[(StatusCode, ErrorResponse)])
//      .recover { case HttpError(_, statusCode) if statusCode == StatusCode.Unauthorized => Left((statusCode, ErrorResponse("Unauthorized"))).withRight[User]}
//    value
//  }

//  def secureEndpoint(authorizationService: AuthorizationService): PartialServerEndpoint[String, User, Unit, (StatusCode, ErrorResponse), Unit, Any, IO] = endpoint
//    .securityIn(auth.bearer[String]())
//    .errorOut(statusCode)
//    .errorOut(jsonBody[ErrorResponse])
//    .serverSecurityLogic(authLogic(authorizationService))
