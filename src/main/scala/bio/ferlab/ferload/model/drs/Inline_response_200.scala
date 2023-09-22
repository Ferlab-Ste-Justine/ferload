package bio.ferlab.ferload.model.drs

import java.util.Date

/**
 * @param `type` 
 * @param id Unique ID of this service. Reverse domain name notation is recommended, though not required. The identifier should attempt to be globally unique so it can be used in downstream aggregator services e.g. Service Registry. for example: ''org.ga4gh.myservice''
 * @param name Name of this service. Should be human readable. for example: ''My project''
 * @param description Description of the service. Should be human readable and provide information about the service. for example: ''This service provides...''
 * @param organization 
 * @param contactUrl URL of the contact for the provider of this service, e.g. a link to a contact form (RFC 3986 format), or an email (RFC 2368 format). for example: ''mailto:support@example.com''
 * @param documentationUrl URL of the documentation of this service (RFC 3986 format). This should help someone learn how to use your service, including any specifics required to access data, e.g. authentication. for example: ''https://docs.myservice.example.com''
 * @param createdAt Timestamp describing when the service was first deployed and available (RFC 3339 format) for example: ''2019-06-04T12:58:19Z''
 * @param updatedAt Timestamp describing when the service was last updated (RFC 3339 format) for example: ''2019-06-04T12:58:19Z''
 * @param environment Environment the service is running in. Use this to distinguish between production, development and testing/staging deployments. Suggested values are prod, test, dev, staging. However this is advised and not enforced. for example: ''test''
 * @param version Version of the service being described. Semantic versioning is recommended, but other identifiers, such as dates or commit hashes, are also allowed. The version should be changed whenever the service is updated. for example: ''1.0.0''
 */
case class Inline_response_200 (
                                 `type`: DrsService_type,
                                 id: String,
                                 name: String,
                                 description: Option[String],
                                 organization: ServiceOrganization,
                                 contactUrl: Option[String],
                                 documentationUrl: Option[String],
                                 createdAt: Option[Date],
                                 updatedAt: Option[Date],
                                 environment: Option[String],
                                 version: String
)

