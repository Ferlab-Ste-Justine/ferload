package bio.ferlab.ferload.model.drs

import bio.ferlab.ferload.model.{ReadResource, ResourceScope, WriteResource}

import java.time.LocalDateTime

case class CreateDrsObject(
                            id: String,
                            name: Option[String],
                            size: Option[Long],
                            created_time: Option[LocalDateTime],
                            updated_time: Option[LocalDateTime],
                            version: Option[String],
                            mime_type: Option[String],
                            checksums: Option[List[Checksum]],
                            description: Option[String],
                            aliases: Option[List[String]],
                            uris: List[String],
                            scopes: Option[List[String]]
                          )

object CreateDrsObject {
  def toResource(obj: CreateDrsObject): WriteResource = {
    val attributes: Map[String, List[String]] = Seq(
      obj.size.map(s => "size" -> List(s.toString)),
      obj.created_time.map(ct => "created_time" -> List(ct.toString)),
      obj.updated_time.map(ut => "updated_time" -> List(ut.toString)),
      obj.version.map(v => "version" -> List(v)),
      obj.mime_type.map(mt => "mime_type" -> List(mt)),
      obj.checksums.map(cs => "checksum" -> cs.map(c => s"${c.`type`}:${c.checksum}")),
      obj.description.map(d => "description" -> List(d)),
      obj.aliases.map(a => "aliases" -> a)
    ).flatten.toMap
    WriteResource(
      id = obj.id,
      name = obj.id,
      displayName = obj.name,
      resourceType = Some("FILE"),
      attributes = attributes,
      uris = obj.uris,
      resourceScopes = obj.scopes
    )
  }
}

