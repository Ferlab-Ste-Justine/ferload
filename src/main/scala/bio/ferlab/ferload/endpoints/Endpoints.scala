package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.Config
import bio.ferlab.ferload.endpoints.ConfigEndpoint.configServerEndpoint
import bio.ferlab.ferload.endpoints.LegacyObjectEndpoints.{objectByPathServer, listObjectsByPathServer}
import bio.ferlab.ferload.endpoints.ObjectsEndpoints.ById.singleObjectServer
import bio.ferlab.ferload.services.{AuthorizationService, ResourceService, S3Service}
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Endpoints:
  private case class User(name: String) extends AnyVal

  val statusEndpoint: PublicEndpoint[Unit, Unit, String, Any] = endpoint.get
    .in("status")
    .out(stringBody)

  val statusServerEndpoint: ServerEndpoint[Any, IO] = statusEndpoint.serverLogicSuccess(_ => IO.pure("OK!"))

  private def apiEndpoints(config: Config, authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): List[ServerEndpoint[Any, IO]] = List(
    statusServerEndpoint,
    configServerEndpoint(config),
  ) ++ ObjectsEndpoints.all(config, authorizationService, resourceService, s3Service)
    ++ DrsEndpoints.all(config, authorizationService, resourceService, s3Service)
    ++ LegacyObjectEndpoints.all(config, authorizationService, s3Service)

  private def docEndpoints(apiEndpoints: List[ServerEndpoint[_, IO]]): List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "ferload", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  private val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def all(config: Config, authorizationService: AuthorizationService, resourceService: ResourceService, s3Service: S3Service): List[ServerEndpoint[Any, IO]] = {
    val api = apiEndpoints(config, authorizationService, resourceService, s3Service)

    docEndpoints(api) ++ api ++ List(metricsEndpoint)
  }


