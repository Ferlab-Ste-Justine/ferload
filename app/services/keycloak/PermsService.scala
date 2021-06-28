package services.keycloak

import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.util.HttpResponseException
import play.api.Configuration

import java.util.Collections
import javax.inject.{Inject, Singleton}

@Singleton
class PermsService @Inject()(config: Configuration) {

  val keycloakConfig = new org.keycloak.authorization.client.Configuration(
    config.get[String]("auth.url"),
    config.get[String]("auth.realm"),
    config.get[String]("auth.client-id"),
    Collections.singletonMap("secret",""), null
  )
  val authzClient = AuthzClient.create(keycloakConfig);

  def checkPermissions(token: String, files: Array[String]): Set[String] = {
    try {
      val perms = authzClient.protection().introspectRequestingPartyToken(token);
      perms.getPermissions.forEach(println)
      files.toSet
    } catch {
          // if 403 user a no permissions at all, return empty set
      case e: HttpResponseException => if(e.getStatusCode==403) Set() else throw e
      case e: Throwable => throw e
    }
  }
}
