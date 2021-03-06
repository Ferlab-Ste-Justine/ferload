package controllers

import auth.{AuthAction, UserRequest}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Logging}
import services.aws.S3Service

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class StatusController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with Logging {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>

    Ok("OK!")

  }


}
