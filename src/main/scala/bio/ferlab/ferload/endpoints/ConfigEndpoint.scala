package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.model.{ClientConfig, FerloadConfig, KeycloakConfig, TokenConfig}
import bio.ferlab.ferload.{Config, FerloadClientConfig}
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
      IO.pure(FerloadConfig(config.ferloadClientConfig.method, None, Some(tokenConfig), None, config.reportApiManifestUrl))
    } else if (config.ferloadClientConfig.method == FerloadClientConfig.PASSWORD) {
      val kc = KeycloakConfig(config.auth.authUrl, config.auth.realm, config.ferloadClientConfig.clientId, config.auth.clientId)
      IO.pure(FerloadConfig(config.ferloadClientConfig.method, Some(kc), None, None, config.reportApiManifestUrl))
    } else if (config.ferloadClientConfig.method == FerloadClientConfig.DEVICE) {
      val kc = KeycloakConfig(config.auth.authUrl, config.auth.realm, config.auth.clientId, config.auth.audience.get)
      val clientConfig = ClientConfig(`manifest-file-pointer` = "File ID", `manifest-filename` = "File Name", `manifest-size` = "File Size")
      IO.pure(FerloadConfig(config.ferloadClientConfig.method, Some(kc), None, Some(clientConfig), config.reportApiManifestUrl))
    }
    else {
      IO.raiseError(new IllegalStateException(s"Invalid configuration type ${config.ferloadClientConfig.method}"))
    }

  })
