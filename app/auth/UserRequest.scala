package auth

import org.keycloak.representations.AccessToken
import play.api.mvc.{Request, WrappedRequest}

case class UserRequest[A](accessToken: AccessToken, token: String, request: Request[A]) extends WrappedRequest[A](request) {
  val isRpt: Boolean = Option(accessToken.getAuthorization).isDefined
}
