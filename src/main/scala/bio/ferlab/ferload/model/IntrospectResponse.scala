package bio.ferlab.ferload.model

import io.circe.{Decoder, HCursor}

case class IntrospectResponse(
                               active: Boolean,
                               exp: Option[Int],
                               iat: Option[Int],
                               aud: Option[Audience],
                               sub: Option[String],
                               azp: Option[String],
                               nbf: Option[Int],
                               authorization: Option[Authorisation]
                             )

case class Authorisation(
                          permissions: Seq[Permissions]
                        )

sealed trait Audience
case class SingleAudience(value: String) extends Audience
case class MultipleAudiences(values: List[String]) extends Audience

object Audience {
  implicit val audienceDecoder: Decoder[Audience] = (c: HCursor) => {
    c.as[String].map(bio.ferlab.ferload.model.SingleAudience.apply) // Try decoding as a single string
      .orElse(c.as[List[String]].map(bio.ferlab.ferload.model.MultipleAudiences.apply)) // Fallback to decoding as a list of strings
  }
}