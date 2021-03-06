package services.keycloak

import auth.UserRequest
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse
import org.keycloak.authorization.client.resource.{AuthorizationResource, ProtectionResource}
import org.keycloak.authorization.client.util.HttpResponseException
import org.keycloak.representations.AccessToken
import org.keycloak.representations.idm.authorization.{AuthorizationResponse, Permission}
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite}
import play.api.Configuration
import play.api.mvc.AnyContent

import scala.jdk.CollectionConverters.SeqHasAsJava

class PermsServiceTest extends FunSuite with BeforeAndAfter{

  // if we want to use the application.conf from resources folder
  // val testConfig: Config = ConfigFactory.load
  // val config = new Configuration(testConfig)

  val config = mock(classOf[Configuration])
  // config mocks
  when(config.get[String]("auth.resources-global-name")).thenReturn("DOWNLOAD")

  val permsClient: PermsClient = mock(classOf[PermsClient])
  val authzResource = mock(classOf[AuthorizationResource])
  val authzResponse = mock(classOf[AuthorizationResponse])
  val authzProtection = mock(classOf[ProtectionResource])
  val tokenIntrospection = mock(classOf[TokenIntrospectionResponse])
  val authzClient: AuthzClient = mock(classOf[AuthzClient])

  val permsService = new PermsService(config, permsClient)
  when(permsClient.authzClient).thenReturn(authzClient)
  val resourcesGlobalName: String = config.get[String]("auth.resources-global-name")

  before {
    // keycloak mocks
    Mockito.reset(authzClient)
    Mockito.reset(authzResource)
    Mockito.reset(authzResponse)
    Mockito.reset(authzProtection)
    Mockito.reset(tokenIntrospection)

    when(authzClient.authorization(anyString)).thenReturn(authzResource)
    when(authzResource.authorize).thenReturn(authzResponse)
    when(authzResponse.getToken).thenReturn("token")
    when(authzClient.protection).thenReturn(authzProtection)
    when(authzProtection.introspectRequestingPartyToken(anyString)).thenReturn(tokenIntrospection)
  }

  private def assertAllowed(files: Set[String], authorized: Set[String], unauthorized: Set[String]) = {
    assert(authorized.nonEmpty)
    assert(unauthorized.isEmpty)
    assert(files.subsetOf(authorized))
  }

  private def assertNotAllowed(files: Set[String], authorized: Set[String], unauthorized: Set[String]) = {
    assert(authorized.isEmpty)
    assert(unauthorized.nonEmpty)
    assert(files.subsetOf(unauthorized))
  }

  test("check-by-resource-allowed") {
    val files = Set("f1", "f2", "f3")
    val permsNames = Set("f1", "f2", "f3", "f4", "f5")
    val (authorized, unauthorized) = permsService.checkByResourcePermissions(files, permsNames)
    assert(authorized.nonEmpty)
    assert(unauthorized.isEmpty)
    assert(files.subsetOf(authorized))
  }

  test("check-by-resource-global") {
    val files = Set("f1", "f2", "f3")
    val permsNames = Set("foo", "bar", "f1", resourcesGlobalName)
    val (authorized, unauthorized) = permsService.checkByResourcePermissions(files, permsNames)
    assert(authorized.nonEmpty)
    assert(unauthorized.isEmpty)
    assert(files.subsetOf(authorized))
  }

  test("check-by-resource-not-allowed") {
    val files = Set("f1", "f2", "f3")
    val permsNames = Set("f1", "f2")
    val (authorized, unauthorized) = permsService.checkByResourcePermissions(files, permsNames)
    assert(authorized.nonEmpty)
    assert(unauthorized.nonEmpty)
    assert(permsNames.subsetOf(authorized))
    assert(unauthorized.contains("f3") && unauthorized.size == 1)
  }

  test("check-perms-global-token") {
    // setup + mocks
    val files = Set("f1", "f2", "f3")
    val permsNames = Set(resourcesGlobalName, "foo")
    val tokenPerms = permsNames.map(name => new Permission(name, name, null, null)).toList.asJava
    when(tokenIntrospection.getPermissions).thenReturn(tokenPerms)
    val userRequest = new UserRequest[AnyContent](new AccessToken(), "token", null)
    // execution
    val (authorized, unauthorized) = permsService.checkPermissions(userRequest, files)
    // assert
    assertAllowed(files, authorized, unauthorized)
  }

  test("check-perms-by-resource-token") {
    // setup + mocks
    val files = Set("f1", "f2", "f3")
    val permsNames = Set("f1", "f2", "f3", "f4", "f5")

    val tokenPerms = permsNames.map(name => new Permission(name, name, null, null)).toList.asJava
    when(tokenIntrospection.getPermissions).thenReturn(tokenPerms)
    val userRequest = new UserRequest[AnyContent](new AccessToken(), "token", null)
    // execution
    val (authorized, unauthorized) = permsService.checkPermissions(userRequest, files)
    // assert
    assertAllowed(files, authorized, unauthorized)
  }

  test("check-perms-http-exception") {
    // setup + mocks
    when(authzClient.authorization(anyString)).thenThrow(new HttpResponseException("something bad", 500, null, null))
    val permsService = new PermsService(config, permsClient)
    val userRequest = new UserRequest[AnyContent](new AccessToken(), "token", null)
    // execution + assert
    assertThrows[HttpResponseException] {
      permsService.checkPermissions(userRequest, null)
    }
  }

  test("check-perms-http-403") {
    // setup + mocks
    val files = Set("f1", "f2", "f3")
    when(authzClient.authorization(anyString)).thenThrow(new HttpResponseException("not allowed at all", 403, null, null))
    val userRequest = new UserRequest[AnyContent](new AccessToken(), "token", null)
    // execution
    val (authorized, unauthorized) = permsService.checkPermissions(userRequest, files)
    // assert
    assertNotAllowed(files, authorized, unauthorized)
  }

  test("check-perms-other-exception") {
    // setup + mocks
    when(authzClient.authorization(anyString)).thenThrow(new IllegalStateException("something bad"))
    val permsService = new PermsService(config, permsClient)
    val userRequest = new UserRequest[AnyContent](new AccessToken(), "token", null)
    // execution + assert
    assertThrows[IllegalStateException] {
      permsService.checkPermissions(userRequest, null)
    }
  }

  test("token has no permissions") {
    // setup + mocks
    val files = Set("f1", "f2", "f3")
    when(tokenIntrospection.getPermissions).thenReturn(null)
    val userRequest = new UserRequest[AnyContent](new AccessToken(), "token", null)
    // execution
    val (authorized, unauthorized) = permsService.checkPermissions(userRequest, files)
    // assert
    assert(authorized.isEmpty)
    assert(unauthorized.nonEmpty)
  }


}
