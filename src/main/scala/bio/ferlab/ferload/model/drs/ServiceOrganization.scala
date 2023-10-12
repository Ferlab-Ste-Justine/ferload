package bio.ferlab.ferload.model.drs

/**
 * Organization providing the service
 *
 * @param name Name of the organization responsible for the service for example: ''My organization''
 * @param url URL of the website of the organization (RFC 3986 format) for example: ''https://example.com''
 */
case class ServiceOrganization(
  name: String,
  url: String
)

