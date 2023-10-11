package bio.ferlab.ferload.model

import sttp.tapir.Schema.annotations.encodedName

import scala.annotation.targetName

case class FerloadConfig(keycloak: KeycloakConfig)
case class KeycloakConfig(url: String, realm: String, `client-id`: String, audience: String)