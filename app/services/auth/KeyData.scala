package services.auth

import play.api.libs.json.{Json, Reads}

case class KeyData(kid: String, n: String, e: String)

object KeyData {
  implicit val reads: Reads[KeyData] = Json.reads[KeyData]
}