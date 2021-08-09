package auth

import org.keycloak.representations.AccessToken
import org.keycloak.representations.AccessToken.Authorization
import org.mockito.Mockito.{mock, when}
import org.scalatest.FunSuite

import java.util

class UserRequestTest extends FunSuite {

  test("isRpt") {
    val accessToken = mock(classOf[AccessToken])
    when(accessToken.getAuthorization).thenReturn(null)
    assert(!UserRequest(accessToken, null, null).isRpt)
    val auth = mock(classOf[Authorization])
    when(accessToken.getAuthorization).thenReturn(auth)
    assert(UserRequest(accessToken, null, null).isRpt)
  }

}
