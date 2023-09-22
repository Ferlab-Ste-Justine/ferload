package bio.ferlab.ferload.model.drs

/**
 * An object that can optionally include information about the error.
 *
 * @param msg A detailed error message.
 * @param status_code The integer representing the HTTP status code (e.g. 200, 404).
 */
case class Error (
  msg: Option[String],
  status_code: Option[Int]
)

