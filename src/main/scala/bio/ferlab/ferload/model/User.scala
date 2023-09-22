package bio.ferlab.ferload.model

case class User(token: String, permissions: Set[Permissions])