package bio.ferlab.ferload.model

import sttp.tapir.Schema.annotations.encodedName

import scala.annotation.targetName

case class FerloadConfig(method: String, keycloak: Option[KeycloakConfig], tokenConfig: Option[TokenConfig])

case class KeycloakConfig(url: String, realm: String, `client-id`: String, audience: String)

case class TokenConfig(realm:String, `client-id`: String, link: String, helper: Option[String])