package services.keycloak

import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.util.HttpResponseException
import play.api.Configuration

import java.util.Collections
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._

@Singleton
class PermsService @Inject()(config: Configuration) {

  val keycloakConfig = new org.keycloak.authorization.client.Configuration(
    config.get[String]("auth.url"),
    config.get[String]("auth.realm"),
    config.get[String]("auth.client-id"),
    Collections.singletonMap("secret",config.get[String]("auth.secret-key")), null
  )
  val authzClient: AuthzClient = AuthzClient.create(keycloakConfig)
  val resourcesPolicy: String = config.get[String]("auth.resources-policy")

  private def checkCQDGPermissions(files: Set[String], permsNames: Set[String]): (Set[String], Set[String]) = {
    val authorized = files.intersect(permsNames)
    val unauthorized = files.diff(permsNames)
    (authorized, unauthorized)
  }

  private def checkCLINPermissions(files: Set[String], permsNames: Set[String]): (Set[String], Set[String]) = {
    if(permsNames.contains("DOWNLOAD")) {
      (files, Set())  // all authorized
    }else{
      (Set(), files)  // all unauthorized
    }
  }

  def checkPermissions(token: String, files: Set[String]): (Set[String], Set[String]) = {
    try {
      // need to convert the user token into authorization token, because the user token is from a public client
      // and authorization token is from the client with credentials where the resources are declared
      val authorizationToken = authzClient.authorization(token).authorize().getToken
      val perms = authzClient.protection().introspectRequestingPartyToken(authorizationToken);
      val permsNames = perms.getPermissions.asScala.map(_.getResourceName).toSet

      resourcesPolicy match {
        case "cqdg" => checkCQDGPermissions(files, permsNames)
        case "clin" => checkCLINPermissions(files, permsNames)
        case _ => throw new RuntimeException(s"Unsupported resources-policy: $resourcesPolicy")
      }

    } catch {
      // if 403 user has no permissions at all, return all files as unauthorized
      case e: HttpResponseException => if(e.getStatusCode==403) (Set(), files) else throw e
      case e: Throwable => throw e
    }
  }
}
