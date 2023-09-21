package bio.ferlab.ferload

import bio.ferlab.ferload.endpoints.Endpoints
import bio.ferlab.ferload.services.AuthorizationService
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.client3.http4s.Http4sBackend
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object Main extends IOApp:

  val config: Config = Config.load()
  private val serverOptions: Http4sServerOptions[IO] =
    Http4sServerOptions
      .customiseInterceptors[IO]
      .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
      .options

  override def run(args: List[String]): IO[ExitCode] = {

    for {
      client <- EmberClientBuilder
        .default[IO]
        .build
      finalClient = Logger(logHeaders = true, logBody = true)(client)
      backend = Http4sBackend.usingClient(client)
      authorizationService = new AuthorizationService(config.auth, backend)
      routes = Http4sServerInterpreter[IO](serverOptions).toRoutes(Endpoints.all(config, authorizationService))
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.http.host).get)
        .withPort(Port.fromInt(config.http.port).get)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build


    } yield ()
  }.useForever





