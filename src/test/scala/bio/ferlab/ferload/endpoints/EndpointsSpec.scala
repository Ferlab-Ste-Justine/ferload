package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.endpoints.Endpoints.statusServerEndpoint
import bio.ferlab.ferload.unwrap
import cats.effect.IO
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.stub.TapirStubInterpreter

class EndpointsSpec extends AnyFlatSpec with Matchers with EitherValues:


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


