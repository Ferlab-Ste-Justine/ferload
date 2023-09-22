package bio.ferlab.ferload.model.drs

/**
 * @param url A fully resolvable URL that can be used to fetch the actual object bytes.
 * @param headers An optional list of headers to include in the HTTP request to `url`. These headers can be used to provide auth tokens required to fetch the object bytes. for example: ''Authorization: Basic Z2E0Z2g6ZHJz''
 */
case class AccessURL (
  url: String,
  headers: Option[List[String]]
)

