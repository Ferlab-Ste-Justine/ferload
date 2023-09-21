package bio.ferlab.ferload.model

import sttp.tapir.Schema.annotations.encodedName

case class FerloadConfig(keycloak: KeycloakConfig)
case class KeycloakConfig(url: String, realm: String, @encodedName("client-id") clientId: String, audience: String)