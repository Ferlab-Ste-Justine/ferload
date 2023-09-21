package bio.ferlab.ferload.model

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import sttp.tapir.Schema.annotations.encodedName
import io.circe.{Decoder, Encoder}

case class ReadResource(id: String,
                        name: String,
                        displayName: Option[String],
                        resourceType: Option[String],
                        attributes: Map[String, List[String]],
                        uris: Seq[String],
                        resourceScopes: Option[Seq[ResourceScope]],
                        scopes: Option[Seq[ResourceScope]]
                       )

object ReadResource {
  implicit val decoder: Decoder[ReadResource] =
    Decoder.forProduct8("_id", "name", "displayName", "type", "attributes", "uris", "resource_scopes", "scopes")(ReadResource.apply)

  implicit val encoder: Encoder[ReadResource] =
    Encoder.forProduct8("_id", "name", "displayName", "type", "attributes", "uris", "resource_scopes", "scopes")(r =>
      (r.id, r.name, r.displayName, r.resourceType, r.attributes, r.uris, r.resourceScopes, r.scopes)
    )
}

case class ResourceScope(name: String)

object ResourceScope {
  implicit val decoder: Decoder[ResourceScope] = deriveDecoder[ResourceScope]
  implicit val encoder: Encoder[ResourceScope] = deriveEncoder[ResourceScope]
}

case class WriteResource(id: String,
                         name: String,
                         displayName: Option[String],
                         resourceType: Option[String],
                         attributes: Map[String, List[String]],
                         uris: Seq[String],
                         resourceScopes: Option[Seq[String]])

object WriteResource {
  implicit val decoder: Decoder[WriteResource] =
    Decoder.forProduct7("_id", "name", "displayName", "type", "attributes", "uris", "resource_scopes")(WriteResource.apply)

  implicit val encoder: Encoder[WriteResource] =
    Encoder.forProduct7("_id", "name", "displayName", "type", "attributes", "uris", "resource_scopes")(r =>
      (r.id, r.name, r.displayName, r.resourceType, r.attributes, r.uris, r.resourceScopes)
    )
}