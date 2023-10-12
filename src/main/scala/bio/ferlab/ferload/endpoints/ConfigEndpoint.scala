package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.{Config, FerloadClientConfig}
import bio.ferlab.ferload.model.{FerloadConfig, KeycloakConfig, TokenConfig}
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.Schema.annotations.encodedName
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

object ConfigEndpoint:


  private val configEndpoint: PublicEndpoint[Unit, Unit, FerloadConfig, Any] = endpoint.get
    .in("config")
    .out(jsonBody[FerloadConfig])

  def configServerEndpoint(config: Config): ServerEndpoint[Any, IO] = configEndpoint.serverLogicSuccess(_ => {
    if (config.ferloadClientConfig.method == FerloadClientConfig.TOKEN) {
      val tokenConfig = TokenConfig(config.auth.realm, config.ferloadClientConfig.clientId, config.ferloadClientConfig.tokenLink.get, config.ferloadClientConfig.tokenHelper)
      IO.pure(FerloadConfig(config.ferloadClientConfig.method, None, Some(tokenConfig)))
    } else if (config.ferloadClientConfig.method == FerloadClientConfig.PASSWORD) {
      val kc = KeycloakConfig(config.auth.authUrl, config.auth.realm, config.ferloadClientConfig.clientId, config.auth.clientId)
      IO.pure(FerloadConfig(config.ferloadClientConfig.method, Some(kc), None))
    }
    else {
      IO.raiseError(new IllegalStateException(s"Invalid configuration type ${config.ferloadClientConfig.method}"))
    }

  })
