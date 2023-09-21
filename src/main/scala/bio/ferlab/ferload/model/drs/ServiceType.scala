package bio.ferlab.ferload.model.drs

/**
 * Type of a GA4GH service
 *
 * @param group Namespace in reverse domain name format. Use `org.ga4gh` for implementations compliant with official GA4GH specifications. For services with custom APIs not standardized by GA4GH, or implementations diverging from official GA4GH specifications, use a different namespace (e.g. your organization's reverse domain name). for example: ''org.ga4gh''
 * @param artifact Name of the API or GA4GH specification implemented. Official GA4GH types should be assigned as part of standards approval process. Custom artifacts are supported. for example: ''beacon''
 * @param version Version of the API or specification. GA4GH specifications use semantic versioning. for example: ''1.0.0''
 */
case class ServiceType (
  group: String,
  artifact: String,
  version: String
)

