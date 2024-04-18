package bio.ferlab.ferload.model

import sttp.tapir.Schema.annotations.encodedName

import scala.annotation.targetName

case class FerloadConfig(method: String, keycloak: Option[KeycloakConfig], tokenConfig: Option[TokenConfig], clientConfig: Option[ClientConfig])

case class KeycloakConfig(url: String, realm: String, `client-id`: String, audience: String)

case class TokenConfig(realm:String, `client-id`: String, link: String, helper: Option[String])

case class ClientConfig(
                          `manifest-file-pointer`: String, 
                          `manifest-filename`: String, 
                          `manifest-size`: String,
                          `manifest-separator`: String = "\t",
                          `download-files-pool`: Int = 10,
                          `download-agreement`: String = "yes",
                          `size-estimation-timeout`: Int = 60
                       )