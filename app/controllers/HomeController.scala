package controllers

import auth.{AuthAction, UserRequest}
import play.api.http.HeaderNames
import play.api.{Configuration, Logging}
import play.api.libs.json.Json
import play.api.mvc._
import services.aws.S3Service
import services.keycloak.PermsService

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,perms: PermsService, s3: S3Service, authAction: AuthAction, config: Configuration) extends BaseController with Logging {

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

  def downloadLinks(): Action[AnyContent] = authAction { implicit request: UserRequest[AnyContent] =>
    val token = request.headers.get(HeaderNames.AUTHORIZATION).get
    val requestedFiles = request.body.asText.get.split("\n")
    val okPermsFiles = perms.checkPermissions(token, requestedFiles)
    val missingPermsFiles = requestedFiles.filter(!okPermsFiles.contains(_))

    if (missingPermsFiles.nonEmpty) {
      Forbidden(missingPermsFiles.mkString("\n"))
    } else {
      val urls = okPermsFiles.map(s3.presignedUrl(bucket, prefix, _))
      Ok(urls.mkString("\n"))
    }
  }

}
