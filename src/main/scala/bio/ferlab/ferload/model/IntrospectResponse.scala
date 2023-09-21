package bio.ferlab.ferload.model

case class IntrospectResponse(active: Boolean, exp: Option[Int], iat: Option[Int], aud: Option[String], nbf: Option[Int], permissions: Option[Seq[Permissions]])