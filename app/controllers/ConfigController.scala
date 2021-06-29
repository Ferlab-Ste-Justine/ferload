package controllers

import play.api.mvc._
import play.api.{Configuration, Logging}
import play.api.libs.json.Json.toJson

import javax.inject._


@Singleton
class ConfigController @Inject()(val controllerComponents: ControllerComponents, val config: Configuration) extends BaseController with Logging {

  private val keycloakUrl = config.get[String]("auth.url")
  private val keycloakRealm = config.get[String]("auth.realm")
  private val keycloakClientId = config.get[String]("auth.public-client-id")

  def configuration(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val configuration = toJson(
      Map(
        "keycloak" ->
          toJson(Map(
            "url" -> toJson(keycloakUrl),
            "realm" -> toJson(keycloakRealm),
            "client-id" -> toJson(keycloakClientId)
          )
        ))
      )

    Ok(configuration)
  }

}
