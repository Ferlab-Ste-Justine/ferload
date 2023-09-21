package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.services.AuthorizationService
import bio.ferlab.ferload.services.AuthorizationService.{ErrorResponse, Permissions, User}
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
  val baseEndpoint: Endpoint[String, Unit, (StatusCode, ErrorResponse), Unit, Any] = endpoint
    .securityIn(auth.bearer[String]())
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))

    
