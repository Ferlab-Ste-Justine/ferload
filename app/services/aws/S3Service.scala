package services.aws

import play.api.Configuration
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

import java.net.{URI, URL}
import java.time.Duration
import javax.inject.{Inject, Singleton}

@Singleton
class S3Service @Inject()(config: Configuration) {
  val confBuilder: S3Configuration = software.amazon.awssdk.services.s3.S3Configuration.builder()
    .pathStyleAccessEnabled(true)
    .checksumValidationEnabled(false)
    .build()
  val staticCredentialsProvider: StaticCredentialsProvider = StaticCredentialsProvider.create(
    AwsBasicCredentials.create(config.get[String]("aws.access_key"), config.get[String]("aws.secret_key"))
  )
  private val endpoint = URI.create(config.get[String]("aws.endpoint"))

  val presigner: S3Presigner = S3Presigner
    .builder()
    .region(Region.CA_CENTRAL_1) //TODO pass in env var
    .endpointOverride(endpoint)
    .serviceConfiguration(confBuilder)
    .credentialsProvider(staticCredentialsProvider)
    .build()


  def presignedUrl(bucket: String, file: String, expiration: Duration = Duration.ofHours(1)): URL = {

    val getObjectRequest =
      GetObjectRequest.builder()
        .bucket(bucket)
        .key(file)
        .build()

    val getObjectPresignRequest = GetObjectPresignRequest.builder
      .signatureDuration(expiration)
      .getObjectRequest(getObjectRequest).build

    val presignedGetObjectRequest =
      presigner.presignGetObject(getObjectPresignRequest)

    val url = presignedGetObjectRequest.url()
    url
  }

}
