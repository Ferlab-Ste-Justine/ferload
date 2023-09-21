package bio.ferlab.ferload.model.drs

import bio.ferlab.ferload.model.ReadResource

import java.time.LocalDateTime

/**
 * @param id             An identifier unique to this `DrsObject`
 * @param name           A string that can be used to name a `DrsObject`. This string is made up of uppercase and lowercase letters, decimal digits, hyphen, period, and underscore [A-Za-z0-9.-_]. See http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap03.html#tag_03_282[portable filenames].
 * @param self_uri       A drs:// hostname-based URI, as defined in the DRS documentation, that tells clients how to access this object. The intent of this field is to make DRS objects self-contained, and therefore easier for clients to store and pass around.  For example, if you arrive at this DRS JSON by resolving a compact identifier-based DRS URI, the `self_uri` presents you with a hostname and properly encoded DRS ID for use in subsequent `access` endpoint calls. for example: ''drs://drs.example.org/314159''
 * @param size           For blobs, the blob size in bytes. For bundles, the cumulative size, in bytes, of items in the `contents` field.
 * @param created_time   Timestamp of content creation in RFC3339. (This is the creation time of the underlying content, not of the JSON object.)
 * @param updated_time   Timestamp of content update in RFC3339, identical to `created_time` in systems that do not support updates. (This is the update time of the underlying content, not of the JSON object.)
 * @param version        A string representing a version. (Some systems may use checksum, a RFC3339 timestamp, or an incrementing version number.)
 * @param mime_type      A string providing the mime-type of the `DrsObject`. for example: ''application/json''
 * @param checksums      The checksum of the `DrsObject`. At least one checksum must be provided. For blobs, the checksum is computed over the bytes in the blob. For bundles, the checksum is computed over a sorted concatenation of the checksums of its top-level contained objects (not recursive, names not included). The list of checksums is sorted alphabetically (hex-code) before concatenation and a further checksum is performed on the concatenated checksum value. For example, if a bundle contains blobs with the following checksums: md5(blob1) = 72794b6d md5(blob2) = 5e089d29 Then the checksum of the bundle is: md5( concat( sort( md5(blob1), md5(blob2) ) ) ) = md5( concat( sort( 72794b6d, 5e089d29 ) ) ) = md5( concat( 5e089d29, 72794b6d ) ) = md5( 5e089d2972794b6d ) = f7a29a04
 * @param access_methods The list of access methods that can be used to fetch the `DrsObject`. Required for single blobs; optional for bundles.
 * @param contents       If not set, this `DrsObject` is a single blob. If set, this `DrsObject` is a bundle containing the listed `ContentsObject` s (some of which may be further nested).
 * @param description    A human readable description of the `DrsObject`.
 * @param aliases        A list of strings that can be used to find other metadata about this `DrsObject` from external metadata sources. These aliases can be used to represent secondary accession numbers or external GUIDs.
 */
case class DrsObject(
                      id: String,
                      name: Option[String],
                      self_uri: String,
                      size: Long,
                      created_time: LocalDateTime,
                      updated_time: Option[LocalDateTime],
                      version: Option[String],
                      mime_type: Option[String],
                      checksums: List[Checksum],
                      access_methods: Option[List[AccessMethod]],
                      contents: Option[List[ContentsObject]],
                      description: Option[String],
                      aliases: Option[List[String]]
                    )

object DrsObject {
  def build(resource: ReadResource, presignedUrl: String, host: String): DrsObject = {

    val accessMethods = AccessMethod(
      `type` = "https",
      access_url = Some(AccessURL(
        url = presignedUrl,
        headers = None
      )),
      access_id = None,
      region = None,
      authorizations = None

    )
    build(resource, host).copy(access_methods = Some(List(accessMethods)))

  }

  def build(resource: ReadResource, host: String): DrsObject = {

    val checksums = resource.attributes.getOrElse("checksum", Nil).map { checksum =>
      val parts = checksum.split(":")
      Checksum(checksum = parts.last, `type` = parts.head)
    }

    DrsObject(
      id = resource.name,
      name = resource.displayName,
      self_uri = s"drs://$host/${resource.name}",
      size = firstAttribute(resource, "size").map(_.toLong).getOrElse(0L),
      created_time = firstAttribute(resource, "created_time").map(LocalDateTime.parse).getOrElse(LocalDateTime.now()),
      updated_time = firstAttribute(resource, "updated_time").map(LocalDateTime.parse),
      version = firstAttribute(resource, "version"),
      mime_type = firstAttribute(resource, "mime_type"),
      checksums = checksums,
      access_methods = None,
      contents = None,
      description = firstAttribute(resource, "description"),
      aliases = resource.attributes.get("aliases")
    )
  }

  private def firstAttribute(resource: ReadResource, key: String): Option[String] = {
    resource.attributes.get(key).flatMap(_.headOption)
  }
}

