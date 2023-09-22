package bio.ferlab.ferload.model

case class PartyToken(access_token: String, expires_in: Int, refresh_expires_in: Int, refresh_token: Option[String], token_type: String)