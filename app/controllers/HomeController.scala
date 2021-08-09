package controllers

import auth.{AuthAction, UserRequest}
import org.keycloak.authorization.client.AuthorizationDeniedException
import play.api.libs.json.Json
import play.api.{Configuration, Logging}
import play.api.libs.json.Json.toJson
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

  def get(file: String): Action[AnyContent] = authAction { implicit request: UserRequest[AnyContent] =>
    val url = s3.presignedUrl(bucket, prefix, file)
    request.getQueryString("format") match {
      case Some("json") =>
        Ok(Json.toJson(Map("url" -> url.toString)))
      case _ =>
        Found(url.toString)
    }
  }

  def downloadLinks(): Action[AnyContent] = authAction { implicit request: UserRequest[AnyContent] =>
    val requestedFiles = extractRequestedFiles(request)
    val (authorized, unauthorized) = perms.checkPermissions(request, requestedFiles)

    if (unauthorized.nonEmpty) {
      Forbidden(unauthorized.mkString("\n"))
    } else {
      val urls = authorized.map(file => (file, s3.presignedUrl(bucket, prefix, file).toString)).toMap
      Ok(toJson(urls))
    }
  }

  def createPermissions(userName: String): Action[AnyContent] = authAction { implicit request: UserRequest[AnyContent] =>
    val requestedFiles = extractRequestedFiles(request)
    try {
      val (created, notCreated) = perms.createPermissions(request.token, userName, requestedFiles)
      if (notCreated.nonEmpty) {
        NotFound(notCreated.mkString("\n"))
      } else {
        Ok(created.mkString("\n"))
      }
    } catch {
      case _: AuthorizationDeniedException => Forbidden(requestedFiles.mkString("\n"))
      case e: Throwable => InternalServerError(s"Failed to create permissions: ${e.getMessage}")
    }
  }

  private def extractRequestedFiles(request: UserRequest[AnyContent]) = {
    request.body.asText.get.split("\n").toSet
  }

}
