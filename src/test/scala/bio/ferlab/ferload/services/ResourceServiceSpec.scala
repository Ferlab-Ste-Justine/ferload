package bio.ferlab.ferload.services

import bio.ferlab.ferload.AuthConfig
import bio.ferlab.ferload.model.{PartyToken, ReadResource, ResourceScope}
import bio.ferlab.ferload.unwrap
import cats.effect.IO
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.HttpError
import sttp.client3.http4s.Http4sBackend
import sttp.client3.testing.RecordingSttpBackend
import sttp.model.StatusCode

class ResourceServiceSpec extends AnyFlatSpec with Matchers with EitherValues {
  "clientToken" should "return a valid token" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(_ => true)
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", "audience", None)

    val resourceService = new ResourceService(authConfig, testingBackend)
    resourceService.clientToken().unwrap shouldBe PartyToken("E123456", 65, 0, None, "bearer")
  }

  it should "return an error if creds are not valid" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(_ => true)
      .thenRespond(""" {"error": "invalid_token"}""", statusCode = StatusCode.Forbidden)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", "audience", None)

    val resourceService = new ResourceService(authConfig, testingBackend)
    val error = the[HttpError[_]] thrownBy {
      resourceService.clientToken().unwrap shouldBe PartyToken("E123456", 65, 0, None, "bearer")
    }

    error should matchPattern {
      case HttpError(_, StatusCode.Forbidden) =>
    }
  }

  "existResource" should "return 200" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "authz", "protection", "resource_set", "F1") && r.method.method == "GET")
      .thenRespond("", statusCode = StatusCode.Ok)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", "audience", None)

    val resourceService = new ResourceService(authConfig, testingBackend)
    resourceService.existResource("F1").unwrap shouldBe StatusCode.Ok
  }

  it should "return 404" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "authz", "protection", "resource_set", "F1"))
      .thenRespond("", statusCode = StatusCode.NotFound)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", "audience", None)

    val resourceService = new ResourceService(authConfig, testingBackend)
    resourceService.existResource("F1").unwrap shouldBe StatusCode.NotFound
  }

  "getResourceById" should "return expected resouce" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "authz", "protection", "resource_set", "FI1"))
      .thenRespond(
        """
          |{
          |    "name": "FI1",
          |    "type": "file",
          |    "owner": {
          |        "id": "c7259bdd-1650-48f4-a626-72e230e85793"
          |    },
          |    "ownerManagedAccess": false,
          |    "displayName": "File Display name FI1",
          |    "attributes": {
          |        "checksum": [
          |            "sha-256:bfbcf769ce225ba55ac3da3bbdd929a71291e16269425c4e83542b7937abf07e",
          |            "md5:c6a2db14d8a78a148e57733584e9865e"
          |        ],
          |        "created_time": [
          |            "2023-09-22T08:55:22"
          |        ],
          |        "description": [
          |            "Hello"
          |        ],
          |        "test": [
          |            "World"
          |        ],
          |        "size": [
          |            "1234"
          |        ]
          |    },
          |    "_id": "ID_FI1",
          |    "uris": [
          |        "s3://cqdg-file-dowbnload/FI1.csv"
          |    ],
          |    "resource_scopes": [
          |        {
          |            "name": "ds1"
          |        }
          |    ],
          |    "scopes": [
          |        {
          |            "name": "ds1"
          |        }
          |    ],
          |    "icon_uri": ""
          |}
          | """.stripMargin, statusCode = StatusCode.Ok)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", "audience", None)

    val resourceService = new ResourceService(authConfig, testingBackend)
    resourceService.getResourceById("FI1").unwrap shouldBe ReadResource(
      "ID_FI1",
      "FI1",
      Some("File Display name FI1"),
      Some("file"),
      Map("test" -> List("World"), "description" -> List("Hello"), "checksum" -> List("sha-256:bfbcf769ce225ba55ac3da3bbdd929a71291e16269425c4e83542b7937abf07e", "md5:c6a2db14d8a78a148e57733584e9865e"), "created_time" -> List("2023-09-22T08:55:22"), "size" -> List("1234")),
      List("s3://cqdg-file-dowbnload/FI1.csv"),
      Some(Seq(ResourceScope("ds1"))),
      Some(Seq(ResourceScope("ds1")))
    )


  }

}
