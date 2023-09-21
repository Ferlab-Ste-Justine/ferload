package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.endpoints.Endpoints.{booksListingServerEndpoint, helloServerEndpoint, statusServerEndpoint}
import bio.ferlab.ferload.endpoints.Library.{Book, books}
import cats.effect.IO
import io.circe.generic.auto.*
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.stub.TapirStubInterpreter

class EndpointsSpec extends AnyFlatSpec with Matchers with EitherValues:

  it should "return hello message" in {
    // given
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(helloServerEndpoint)
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://test.com/hello?name=john")
      .send(backendStub)

    // then
    response.map(_.body.value shouldBe "Hello john").unwrap
  }

  it should "list available books" in {
    // given
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(booksListingServerEndpoint)
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://test.com/books/list/all")
      .response(asJson[List[Book]])
      .send(backendStub)

    // then
    response.map(_.body.value shouldBe books).unwrap
  }

  "status" should "return ok" in {
    //given
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(statusServerEndpoint)
      .backend()
    // when
    val response = basicRequest
      .get(uri"http://test.com/status")
      .send(backendStub)

    response.map(_.body.value shouldBe "OK!").unwrap
  }


