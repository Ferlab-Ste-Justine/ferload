package bio.ferlab.ferload.model

case class Permissions(resource_id: String, rsname: Option[String], resource_scopes: Seq[String])