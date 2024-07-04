package bio.ferlab.ferload.services

import bio.ferlab.ferload.AuthConfig
import bio.ferlab.ferload.unwrap
import bio.ferlab.ferload.model.{Authorisation, ErrorResponse, IntrospectResponse, Permissions, User}
import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.http4s.Http4sBackend
import sttp.client3.testing.RecordingSttpBackend
import sttp.client3.{HttpError, StringBody, UriContext}
import sttp.model.{MediaType, StatusCode}

class AuthorizationServiceSpec extends AnyFlatSpec with Matchers with EitherValues {

  "requestPartyToken" should "return a token" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(_ => true)
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", None, None)
    val authorizationService = new AuthorizationService(authConfig, testingBackend)
    authorizationService.requestPartyToken("https://ferlab.bio", Seq("FI1")).unwrap shouldBe "E123456"
    testingBackend.allInteractions.size shouldBe 1
    val (request, _) = testingBackend.allInteractions.head
    request.uri shouldBe uri"http://stub.local/realms/realm/protocol/openid-connect/token"
    request.body.asInstanceOf[StringBody] shouldBe StringBody("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Auma-ticket&audience=clientId&permission=FI1", "utf-8", MediaType("text", "plain"))

  }

  it should "return an error" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(_ => true)
      .thenRespond(""" {"error": "invalid_token"}""", statusCode = StatusCode.Forbidden)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", None, None)
    val authorizationService = new AuthorizationService(authConfig, testingBackend)
    a[HttpError[_]] should be thrownBy {
      authorizationService.requestPartyToken("https://ferlab.bio", Seq("FI1")).unwrap
    }

    testingBackend.allInteractions.size shouldBe 1
    val (request, _) = testingBackend.allInteractions.head
    request.uri shouldBe uri"http://stub.local/realms/realm/protocol/openid-connect/token"
    request.body.asInstanceOf[StringBody] shouldBe StringBody("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Auma-ticket&audience=clientId&permission=FI1", "utf-8", MediaType("text", "plain"))
  }

  "introspectPartyToken" should "return a resonse token" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(_ => true)
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1 Name",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )

    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", None, None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)
    val resp = authorizationService.introspectToken("E123456").unwrap
    resp shouldBe IntrospectResponse(active = true, exp = Some(65), iat = Some(20), aud = Some("cqdg"), sub = None, azp = None, nbf = Some(4), authorization = Some(Authorisation(Seq(Permissions("F1", Some("F1 Name"), Seq("Scope1", "Scope2"))))))


  }

  "authLogic" should "return a User" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )

    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    authorizationService.authLogic("token", Seq("F1"), "token").unwrap.value._1 shouldBe User("token", Set(Permissions("F1", Some("F1"), Seq("Scope1", "Scope2"))))
  }

  it should "return a forbidden if user dont have access to all resources" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "azp": "allowed_client",
          | "nbf": 4,
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )

    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    authorizationService.authLogic("token", Seq("F1", "F2"), "token").unwrap.left.value shouldBe (StatusCode.Forbidden,  ErrorResponse("[F2]", 403))
  }

  it should "return a forbidden if user dont have access any resources" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(""" {
                     |    "error": "access_denied",
                     |    "error_description": "not_authorized"
                     |} """.stripMargin, StatusCode.Forbidden)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    authorizationService.authLogic("token", Seq("F1"), "token").unwrap.left.value shouldBe(StatusCode.Forbidden, ErrorResponse("Unauthorized", 403))
  }

  it should "return a resource not found if resource does not exist" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(
        """ {
          |    "error": "invalid_resource",
          |    "error_description": "Resource with id [FIdssda] does not exist."
          |} """.stripMargin, StatusCode.BadRequest)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    authorizationService.authLogic("token", Seq("F1"), "token").unwrap.left.value shouldBe(StatusCode.NotFound, ErrorResponse("Not Found", 404))
  }

  it should "return unauthorized if bearer token is not valid" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(
        """ {
          |    "error": "invalid_grant",
          |    "error_description": "Invalid bearer token"
          |} """.stripMargin, StatusCode.Unauthorized)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    authorizationService.authLogic("token", Seq("F1"), "token").unwrap.left.value shouldBe(StatusCode.Unauthorized, ErrorResponse("Unauthorized", 401))
  }

  // authLogic does return pre-sign url, only authorized client should be granted access
  it should "return forbidden for any client other then the authorized client for device method" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )
    val authConfigOk = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationServiceOk = new AuthorizationService(authConfigOk, testingBackend)

    // match client: allowed_client
    authorizationServiceOk.authLogic("token", Seq("F1"), "device").unwrap.value._1 shouldBe User("token", Set(Permissions("F1", Some("F1"), Seq("Scope1", "Scope2"))))

    val authConfigNotOk = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("other_allowed_client"), None)

    val authorizationServiceNotOk = new AuthorizationService(authConfigNotOk, testingBackend)

    // not matching clients: allowed_client vs. other_allowed_client
    authorizationServiceNotOk.authLogic("token", Seq("F1"), "device").unwrap.left.value shouldBe(StatusCode.Forbidden, ErrorResponse("Unauthorized", 403))

  }

  it should "return User for any valid client for method other and device" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )

    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("other_allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    // not matching clients: allowed_client vs. other_allowed_client
    authorizationService.authLogic("token", Seq("F1"), "token").unwrap.value._1 shouldBe User("token", Set(Permissions("F1", Some("F1"), Seq("Scope1", "Scope2"))))

  }

  "authLogicAuthorizationForUser" should "return a User" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token"))
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )

    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    val inputJson = parse("""{"file_ids":["FI1"]}""".stripMargin).getOrElse(Json.Null)

    authorizationService.authLogicAuthorizationForUser("token", inputJson).unwrap.value shouldBe User("token", Set(Permissions("F1", Some("F1"), Seq("Scope1", "Scope2"))))
  }

  it should "return only resources ids the user has access" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )

    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    val inputJson = parse("""{"file_ids":["FI1","F2"]}""".stripMargin).getOrElse(Json.Null)

    authorizationService.authLogicAuthorizationForUser("token", inputJson).unwrap.value shouldBe User("token", Set(Permissions("F1", Some("F1"), List("Scope1", "Scope2"))))
  }

  it should "return unauthorized if bearer token is not valid" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(
        """ {
          |    "error": "invalid_grant",
          |    "error_description": "Invalid bearer token"
          |} """.stripMargin, StatusCode.Unauthorized)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )
    val authConfig = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationService = new AuthorizationService(authConfig, testingBackend)

    val inputJson = parse("""{"file_ids":["FI1"]}""".stripMargin).getOrElse(Json.Null)

    authorizationService.authLogicAuthorizationForUser("token", inputJson).unwrap.left.value shouldBe(StatusCode.Unauthorized, ErrorResponse("Unauthorized", 401))
  }

  // authLogicAuthorizationForUser does not return any pre-sign url, all valid clients should be able to access
  it should "authorize for any valid client" in {
    val testingBackend = new RecordingSttpBackend(Http4sBackend.stub[IO]
      .whenRequestMatches(r => {
        r.uri.path == Seq("realms", "realm", "protocol", "openid-connect", "token")
      })
      .thenRespond(""" {"access_token": "E123456", "expires_in": 65, "refresh_expires_in": 0, "token_type" : "bearer"} """)
      .whenRequestMatches(r => r.uri.path.contains("introspect"))
      .thenRespond(
        """ {
          | "active": true,
          | "exp": 65,
          | "iat": 20,
          | "aud" : "cqdg",
          | "nbf": 4,
          | "azp": "allowed_client",
          | "authorization":{
          | "permissions" : [
          |   {
          |     "rsid": "F1",
          |     "rsname": "F1",
          |     "scopes": ["Scope1", "Scope2"]
          |   }
          | ]
          | }
          |} """.stripMargin)
    )
    val authConfigOne = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("allowed_client"), None)

    val authorizationServiceOne = new AuthorizationService(authConfigOne, testingBackend)

    val inputJsonOne = parse("""{"file_ids":["FI1"]}""".stripMargin).getOrElse(Json.Null)

    authorizationServiceOne.authLogicAuthorizationForUser("token", inputJsonOne).unwrap.value shouldBe User("token", Set(Permissions("F1", Some("F1"), Seq("Scope1", "Scope2"))))

    //------------------------------------

    val authConfigTwo = AuthConfig("http://stub.local", "realm", "clientId", "clientSecret", Some("other_allowed_client"), None)

    val authorizationServiceTwo = new AuthorizationService(authConfigTwo, testingBackend)

    val inputJsonTwo = parse("""{"file_ids":["FI1"]}""".stripMargin).getOrElse(Json.Null)

    authorizationServiceTwo.authLogicAuthorizationForUser("token", inputJsonTwo).unwrap.value shouldBe User("token", Set(Permissions("F1", Some("F1"), Seq("Scope1", "Scope2"))))

  }
}
