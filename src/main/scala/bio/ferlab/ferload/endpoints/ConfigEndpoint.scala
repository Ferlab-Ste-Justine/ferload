package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.Config
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.Schema.annotations.encodedName
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
object ConfigEndpoint:

  case class FerloadConfig(keycloak: KeycloakConfig)
  case class KeycloakConfig(url: String, realm: String, @encodedName("client-id") clientId: String, audience: String)

  private val configEndpoint: PublicEndpoint[Unit, Unit, FerloadConfig, Any] = endpoint.get
    .in("config")
    .out(jsonBody[FerloadConfig])

  def configServerEndpoint(config: Config): ServerEndpoint[Any, IO] = configEndpoint.serverLogicSuccess(_ => {
    val kc = KeycloakConfig(config.auth.authUrl, config.auth.realm, config.auth.clientId, config.auth.audience)
    IO.pure(FerloadConfig(kc))
  })
