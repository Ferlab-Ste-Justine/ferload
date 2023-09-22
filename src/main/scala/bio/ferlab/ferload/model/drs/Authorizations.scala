package bio.ferlab.ferload.model.drs

/**
 * @param supported_types An Optional list of support authorization types. More than one can be supported and tried in sequence. Defaults to `None` if empty or missing.
 * @param passport_auth_issuers If authorizations contain `PassportAuth` this is a required list of visa issuers (as found in a visa's `iss` claim) that may authorize access to this object. The caller must only provide passports that contain visas from this list. It is strongly recommended that the caller validate that it is appropriate to send the requested passport/visa to the DRS server to mitigate attacks by malicious DRS servers requesting credentials they should not have.
 * @param bearer_auth_issuers If authorizations contain `BearerAuth` this is an optional list of issuers that may authorize access to this object. The caller must provide a token from one of these issuers. If this is empty or missing it assumed the caller knows which token to send via other means. It is strongly recommended that the caller validate that it is appropriate to send the requested token to the DRS server to mitigate attacks by malicious DRS servers requesting credentials they should not have.
 */
case class Authorizations (
  supported_types: Option[List[String]],
  passport_auth_issuers: Option[List[String]],
  bearer_auth_issuers: Option[List[String]]
)

