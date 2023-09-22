package bio.ferlab.ferload.model

case class Resource(name: String, displayName: Option[String], attributes: Map[String, List[String]], uris: Seq[String])