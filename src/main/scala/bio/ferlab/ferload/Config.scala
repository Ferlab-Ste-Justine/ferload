package bio.ferlab.ferload


case class Config(auth: AuthConfig, http: HttpConfig, s3Config: S3Config, drsConfig: DrsConfig)

case class S3Config(
                     accessKey: Option[String],
                     secretKey: Option[String],
                     endpoint: Option[String],
                     defaultBucket: Option[String],
                     pathAccessStyle: Boolean,
                     region: Option[String],
                     expirationPresignedUrlInSeconds: Int
                   )

object S3Config {
  def load(): S3Config = {
    S3Config(
      sys.env.get("AWS_ACCESS_KEY"),
      sys.env.get("AWS_SECRET_KEY"),
      sys.env.get("AWS_ENDPOINT"),
      sys.env.get("AWS_BUCKET"),
      sys.env.get("AWS_PATH_ACCESS_STYLE").exists(_.toBoolean),
      sys.env.get("AWS_REGION"),
      sys.env.get("AWS_PRESIGNED_URL_EXPIRATION_IN_SECONDS").map(_.toInt).getOrElse(3600)
    )
  }
}

case class DrsConfig(
                      id: String,
                      name: String,
                      version: String,
                      organizationName: String,
                      organizationUrl: String,
                      description: Option[String] = None,
                      contactUrl: Option[String] = None,
                      documentationUrl: Option[String] = None,
                      environment: Option[String] = None,
                    )

object DrsConfig {
  def load(): DrsConfig = {
    DrsConfig(
      sys.env("DRS_ID"),
      sys.env("DRS_NAME"),
      sys.env.getOrElse("DRS_VERSION", "1.3.0"),
      sys.env("DRS_ORGANIZATION_NAME"),
      sys.env("DRS_ORGANIZATION_URL"),
      sys.env.get("DRS_DESCRIPTION"),
      sys.env.get("DRS_CONTACT_URL"),
      sys.env.get("DRS_DOCUMENTATION_URL"),
      sys.env.get("DRS_ENVIRONMENT"),

    )
  }
}

case class HttpConfig(host: String, port: Int)

object HttpConfig {
  val DEFAULT_PORT = 9090
  val DEFAULT_HOST = "localhost"

  def load(): HttpConfig = {
    val port = sys.env
      .get("HTTP_PORT")
      .map(_.toInt)
      .getOrElse(DEFAULT_PORT)
    val host = sys.env
      .getOrElse("HTTP_HOST", DEFAULT_HOST)
    HttpConfig(host, port)
  }
}

case class AuthConfig(authUrl: String, realm: String, clientId: String, clientSecret: String, audience: String, resourcesGlobalName: Option[String])


object Config {
  def load(): Config = {

    Config(
      AuthConfig(
        sys.env("AUTH_URL"),
        sys.env("AUTH_REALM"),
        sys.env("AUTH_CLIENT_ID"),
        sys.env("AUTH_SECRET_KEY"),
        sys.env("AUTH_AUDIENCE"),
        sys.env.get("AUTH_RESOURCES_POLICY_GLOBAL_NAME")
      ),
      HttpConfig.load(),
      S3Config.load(),
      DrsConfig.load()
    )


  }
}
