package bio.ferlab.ferload.model.drs

/**
 * @param `type` Type of the access method.
 * @param access_url 
 * @param access_id An arbitrary string to be passed to the `/access` method to get an `AccessURL`. This string must be unique within the scope of a single object. Note that at least one of `access_url` and `access_id` must be provided.
 * @param region Name of the region in the cloud service provider that the object belongs to. for example: ''us-east-1''
 * @param authorizations 
 */
case class AccessMethod (
  `type`: String,
  access_url: Option[AccessURL],
  access_id: Option[String],
  region: Option[String],
  authorizations: Option[AllOfAccessMethodAuthorizations]
)

