package bio.ferlab.ferload.model.drs

/**
 * @param checksum The hex-string encoded checksum for the data
 * @param `type` The digest method used to create the checksum. The value (e.g. `sha-256`) SHOULD be listed as `Hash Name String` in the https://www.iana.org/assignments/named-information/named-information.xhtml#hash-alg[IANA Named Information Hash Algorithm Registry]. Other values MAY be used, as long as implementors are aware of the issues discussed in https://tools.ietf.org/html/rfc6920#section-9.4[RFC6920]. GA4GH may provide more explicit guidance for use of non-IANA-registered algorithms in the future. Until then, if implementers do choose such an algorithm (e.g. because it's implemented by their storage provider), they SHOULD use an existing standard `type` value such as `md5`, `etag`, `crc32c`, `trunc512`, or `sha1`. for example: ''sha-256''
 */
case class Checksum (
  checksum: String,
  `type`: String
)

