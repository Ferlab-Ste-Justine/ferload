package bio.ferlab.ferload.model

case class IntrospectResponse(
                               active: Boolean,
                               exp: Option[Int],
                               iat: Option[Int],
                               aud: Option[String],
                               sub: Option[String],
                               azp: Option[String],
                               nbf: Option[Int],
                               authorization: Option[Authorisation]
                             )

case class Authorisation(
                          permissions: Seq[Permissions]
                        )