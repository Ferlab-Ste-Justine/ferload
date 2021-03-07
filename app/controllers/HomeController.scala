package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.S3Service

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, val s3: S3Service) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def igv(id:String) = Action { implicit request: Request[AnyContent] =>

    Ok(views.html.index(s"$id.cram", s"$id.cram.crai"))

  }


  def download(file: String) = Action { implicit request: Request[AnyContent] =>
    val url = s3.presignedUrl("clin-repository", file)
    TemporaryRedirect(url.toString)
  }

  def presignedUrl(file: String) = Action { implicit request: Request[AnyContent] =>
    val url = s3.presignedUrl("clin-repository", file)
    Ok(Json.toJson(Map("url" -> url.toString)))

  }

}
