package bio.ferlab.ferload.endpoints

import ConfigEndpoint.configServerEndpoint
import FileEndpoint.{fileServerEndpoint, filesServerEndpoint}
import Library.*
import bio.ferlab.ferload.Config
import bio.ferlab.ferload.services.AuthorizationService
import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.client.Client
import sttp.tapir.*
import sttp.tapir.Schema.annotations.encodedName
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Endpoints:
  private case class User(name: String) extends AnyVal

  private val statusEndpoint: PublicEndpoint[Unit, Unit, String, Any] = endpoint.get
    .in("status")
    .out(stringBody)

  val statusServerEndpoint: ServerEndpoint[Any, IO] = statusEndpoint.serverLogicSuccess(_ => IO.pure("OK!"))

  private val helloEndpoint: PublicEndpoint[User, Unit, String, Any] = endpoint.get
    .in("hello")
    .in(query[User]("name"))
    .out(stringBody)
  val helloServerEndpoint: ServerEndpoint[Any, IO] = helloEndpoint.serverLogicSuccess(user => IO.pure(s"Hello ${user.name}"))

  private val booksListing: PublicEndpoint[Unit, Unit, List[Book], Any] = endpoint.get
    .in("books" / "list" / "all")
    .out(jsonBody[List[Book]])
  val booksListingServerEndpoint: ServerEndpoint[Any, IO] = booksListing.serverLogicSuccess(_ => IO.pure(Library.books))

  private def apiEndpoints(config: Config, authorizationService: AuthorizationService): List[ServerEndpoint[Any, IO]] = List(
    helloServerEndpoint,
    booksListingServerEndpoint,
    statusServerEndpoint,
    configServerEndpoint(config),
    filesServerEndpoint(config, authorizationService),
    fileServerEndpoint(config, authorizationService)
  )

  private def docEndpoints(apiEndpoints: List[ServerEndpoint[_, IO]]): List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "ferload", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  private val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def all(config: Config, authorizationService: AuthorizationService): List[ServerEndpoint[Any, IO]] = {
    val api = apiEndpoints(config, authorizationService)

    docEndpoints(api) ++ api ++ List(metricsEndpoint)
  }

object Library:
  case class Author(name: String)

  case class Book(title: String, year: Int, author: Author)

  val books: List[Book] = List(
    Book("The Sorrows of Young Werther", 1774, Author("Johann Wolfgang von Goethe")),
    Book("On the Niemen", 1888, Author("Eliza Orzeszkowa")),
    Book("The Art of Computer Programming", 1968, Author("Donald Knuth")),
    Book("Pharaoh", 1897, Author("Boleslaw Prus"))
  )
