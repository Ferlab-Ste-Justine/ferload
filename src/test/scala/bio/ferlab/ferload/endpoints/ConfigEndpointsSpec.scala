package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.endpoints.ConfigEndpoint.{FerloadConfig, KeycloakConfig, configServerEndpoint}
import bio.ferlab.ferload.{AuthConfig, Config, HttpConfig, S3Config}
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

class ConfigEndpointsSpec extends AnyFlatSpec with Matchers with EitherValues:

  "config" should "return expected config" in {
    //given
    val config = Config(AuthConfig("http://localhost:8080", "realm", "clientId", "clientSecret", "audience", None), HttpConfig("localhost", 9090), S3Config(Some("accessKey"), Some("secretKey"), Some("endpoint"), Some("bucket"), false, Some("region"), 3600))
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(configServerEndpoint(config))
      .backend()
    // when
    val response = basicRequest
      .get(uri"http://test.com/config")
      .response(asJson[FerloadConfig])
      .send(backendStub)

    val expected = FerloadConfig(KeycloakConfig("http://localhost:8080", "realm", "clientId", "audience"))
    response.map(_.body.value shouldBe expected).unwrap
  }

