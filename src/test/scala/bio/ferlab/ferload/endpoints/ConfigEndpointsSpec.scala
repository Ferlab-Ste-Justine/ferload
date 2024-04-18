package bio.ferlab.ferload.endpoints

import bio.ferlab.ferload.endpoints.ConfigEndpoint.configServerEndpoint
import bio.ferlab.ferload.model.{ClientConfig, FerloadConfig, KeycloakConfig, TokenConfig}
import bio.ferlab.ferload.{AuthConfig, Config, DrsConfig, FerloadClientConfig, HttpConfig, S3Config, unwrap}
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

  "config" should "return expected config for password method" in {
    //given
    val config = Config(
      AuthConfig("http://localhost:8080", "realm", "clientId", "clientSecret", None, None),
      HttpConfig("localhost", 9090),
      S3Config(Some("accessKey"), Some("secretKey"), Some("endpoint"), Some("bucket"), false, Some("region"), 3600),
      DrsConfig("ferlaod", "Ferload", "ferload.ferlab.bio", "1.3.0", "Ferlab", "https://ferlab.bio"),
      FerloadClientConfig(FerloadClientConfig.PASSWORD, "ferloadClientId", None, None)
    )
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(configServerEndpoint(config))
      .backend()
    // when
    val response = basicRequest
      .get(uri"http://test.com/config")
      .response(asJson[FerloadConfig])
      .send(backendStub)

    val expected = FerloadConfig(FerloadClientConfig.PASSWORD, Some(KeycloakConfig("http://localhost:8080", "realm", "ferloadClientId", "clientId")), None, None)
    response.map(_.body.value shouldBe expected).unwrap
  }

  it should "return expected config for token method" in {
    //given
    val config = Config(
      AuthConfig("http://localhost:8080", "realm", "clientId", "clientSecret", None, None),
      HttpConfig("localhost", 9090),
      S3Config(Some("accessKey"), Some("secretKey"), Some("endpoint"), Some("bucket"), false, Some("region"), 3600),
      DrsConfig("ferlaod", "Ferload", "ferload.ferlab.bio", "1.3.0", "Ferlab", "https://ferlab.bio"),
      FerloadClientConfig(FerloadClientConfig.TOKEN, "ferloadClientId", Some("https://ferload.ferlab.bio/token"), Some("Please copy / paste this url in your browser to get a new authentication token."))
    )
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(configServerEndpoint(config))
      .backend()
    // when
    val response = basicRequest
      .get(uri"http://test.com/config")
      .response(asJson[FerloadConfig])
      .send(backendStub)

    val expected = FerloadConfig(
      FerloadClientConfig.TOKEN, 
      None, 
      Some(TokenConfig(
        "realm", 
        "ferloadClientId", 
        "https://ferload.ferlab.bio/token", 
        Some("Please copy / paste this url in your browser to get a new authentication token."),
        
      )),
      None
    )
    response.map(_.body.value shouldBe expected).unwrap
  }

  it should "return expected config for device method" in {
    //given
    val config = Config(
      AuthConfig("http://localhost:8080", "realm", "resource_client", "clientSecret", Some("cqdg_acl"), None),
      HttpConfig("localhost", 9090),
      S3Config(Some("accessKey"), Some("secretKey"), Some("endpoint"), Some("bucket"), false, Some("region"), 3600),
      DrsConfig("ferlaod", "Ferload", "ferload.ferlab.bio", "1.3.0", "Ferlab", "https://ferlab.bio"),
      FerloadClientConfig(FerloadClientConfig.DEVICE, "ferloadClientId", Some("https://ferload.ferlab.bio/token"), Some("Please copy / paste this url in your browser to get a new authentication token."))
    )
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(configServerEndpoint(config))
      .backend()
    // when
    val response = basicRequest
      .get(uri"http://test.com/config")
      .response(asJson[FerloadConfig])
      .send(backendStub)

    val expected = FerloadConfig(
      FerloadClientConfig.DEVICE,
      Some(KeycloakConfig("http://localhost:8080", "realm", "resource_client", "cqdg_acl")),
      None,
      Some(ClientConfig(`manifest-file-pointer` = "File ID", `manifest-filename` = "File Name", `manifest-size` = "File Size"))
    )
    response.map(_.body.value shouldBe expected).unwrap
  }