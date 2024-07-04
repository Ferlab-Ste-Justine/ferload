package bio.ferlab.ferload.model

case class Permissions(rsid: String, rsname: Option[String], scopes: Seq[String])