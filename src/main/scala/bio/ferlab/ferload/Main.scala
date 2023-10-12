package bio.ferlab.ferload

import bio.ferlab.ferload.endpoints.Endpoints
import bio.ferlab.ferload.services.{AuthorizationService, ResourceService, S3Service}
import cats.effect.{ExitCode, IO, IOApp}
import ch.qos.logback.classic.Level
import com.comcast.ip4s.{Host, Port}
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.client3.http4s.Http4sBackend
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import org.http4s.server.middleware.*
import org.slf4j.LoggerFactory

object Main extends IOApp:

  val config: Config = Config.load()
  private val serverOptions: Http4sServerOptions[IO] =
    Http4sServerOptions
      .customiseInterceptors[IO]
      .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
      .options

  override def run(args: List[String]): IO[ExitCode] = {
    setLogLevel()
    for {
      client: Client[IO] <- EmberClientBuilder
        .default[IO]
        .build
      finalClient = setHttpClientLogger(client)
      backend = Http4sBackend.usingClient(client)
      authorizationService = new AuthorizationService(config.auth, backend)
      resourceService = new ResourceService(config.auth, backend)
      s3Service = new S3Service(config.s3Config)
      routes = Http4sServerInterpreter[IO](serverOptions).toRoutes(Endpoints.all(config, authorizationService, resourceService, s3Service))
      withCors = CORS.policy.withAllowOriginAll(routes)
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.http.host).get)
        .withPort(Port.fromInt(config.http.port).get)
        .withHttpApp(Router("/" -> withCors).orNotFound)
        .build


    } yield ()
  }.useForever

def setLogLevel(): Unit = {
  sys.env.get("LOG_LEVEL")
    .foreach { envLogLeve =>
      import ch.qos.logback.classic.Logger
      val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
      rootLogger.setLevel(Level.toLevel(envLogLeve, Level.WARN))
    }

}

def setHttpClientLogger(client: Client[IO]): Client[IO] = {
  sys.env.get("LOG_LEVEL") match
    case Some("DEBUG") => Logger(logHeaders = true, logBody = true)(client)
    case _ => client
}



