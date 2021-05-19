package controllers

import auth.{AuthAction, UserRequest}
import play.api.{Configuration, Logging}
import play.api.libs.json.Json
import play.api.mvc._
import services.aws.S3Service

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, s3: S3Service, authAction: AuthAction, config: Configuration) extends BaseController with Logging {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def igv(id: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>

    Ok(views.html.index(s"$id.cram", s"$id.cram.crai"))

  }

  private val bucket = config.get[String]("aws.bucket")
  private val prefix = config.get[String]("aws.prefix")

  def presignedUrl(file: String): Action[AnyContent] = authAction { implicit request: UserRequest[AnyContent] =>
    val url = s3.presignedUrl(bucket, prefix, file)
    Ok(Json.toJson(Map("url" -> url.toString)))
  }

  def download(file: String): Action[AnyContent] = authAction { implicit request: UserRequest[AnyContent] =>
    val url = s3.presignedUrl(bucket, prefix, file)

    Found(url.toString)
  }

}
