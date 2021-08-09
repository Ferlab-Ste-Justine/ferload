package services.keycloak

import org.keycloak.authorization.client.AuthzClient
import play.api.Configuration

import java.util.Collections
import javax.inject.{Inject, Singleton}

@Singleton
class PermsClient @Inject()(config: Configuration) {

    val keycloakConfig = new org.keycloak.authorization.client.Configuration(
      config.get[String]("auth.url"),
      config.get[String]("auth.realm"),
      config.get[String]("auth.client-id"),
      Collections.singletonMap("secret",config.get[String]("auth.secret-key")), null
    )

    val authzClient: AuthzClient = AuthzClient.create(keycloakConfig)

}
