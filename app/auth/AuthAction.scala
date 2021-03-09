package auth

import org.keycloak.common.VerificationException
import play.api.http.HeaderNames
import play.api.mvc._
import services.auth.AuthService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


// A custom request type to hold our JWT claims, we can pass these on to the
// handling action


// Our custom action implementation
class AuthAction @Inject()(bodyParser: BodyParsers.Default, authService: AuthService)(implicit ec: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent] {

  override def parser: BodyParser[AnyContent] = bodyParser

  override protected def executionContext: ExecutionContext = ec

  // A regex for parsing the Authorization header value
  private val headerTokenRegex = """Bearer (.+?)""".r

  // Called when a request is invoked. We should validate the bearer token here
  // and allow the request to proceed if it is valid.
  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] =
    extractBearerToken(request) map { token =>
      authService.verifyToken(token)
        .flatMap { accessToken => block(auth.UserRequest(accessToken, token, request)) }
        .recover { case e: VerificationException => Results.Unauthorized(e.getMessage) }
    } getOrElse Future.successful(Results.Unauthorized) // no token was sent - return 401

  // Helper for extracting the token value
  private def extractBearerToken[A](request: Request[A]): Option[String] =
    request.headers.get(HeaderNames.AUTHORIZATION) collect {
      case headerTokenRegex(token) => token
    }
}