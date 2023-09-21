package bio.ferlab.ferload.services

import bio.ferlab.ferload.S3Config
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentialsProvider, InstanceProfileCredentialsProvider, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

import java.net.URI
import java.time.Duration
import scala.util.{Failure, Success, Try}

class S3Service(s3Config: S3Config) {
  val confBuilder: S3Configuration = software.amazon.awssdk.services.s3.S3Configuration.builder()
    .pathStyleAccessEnabled(s3Config.pathAccessStyle)
    .build()
  val credentialsProvider: AwsCredentialsProvider = s3Config.accessKey.map { accessKey =>
    StaticCredentialsProvider.create(
      AwsBasicCredentials.create(accessKey, s3Config.secretKey.get)
    )
  }.getOrElse(InstanceProfileCredentialsProvider.create())

  private val endpoint: Option[URI] = s3Config.endpoint.map(URI.create)

  private val presignerBuilder = S3Presigner
    .builder()

  private val regionPresignerBuilder = s3Config.region.map(r => presignerBuilder.region(Region.of(r))).getOrElse(presignerBuilder)

  private val endpointPresignerBuilder = endpoint.map(regionPresignerBuilder.endpointOverride).getOrElse(regionPresignerBuilder)

  val presigner: S3Presigner = endpointPresignerBuilder
    .serviceConfiguration(confBuilder)
    .credentialsProvider(credentialsProvider)
    .build()

  private val presignedUrlDuration: Duration = Duration.ofSeconds(s3Config.expirationPresignedUrlInSeconds)


  def presignedUrl(bucket: String, file: String): String = {

    val getObjectRequest =
      GetObjectRequest.builder()
        .bucket(bucket)
        .key(file)
        .build()

    val getObjectPresignRequest = GetObjectPresignRequest.builder
      .signatureDuration(presignedUrlDuration)
      .getObjectRequest(getObjectRequest).build

    val presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest)

    val url = presignedGetObjectRequest.url()
    url.toString
  }


}

object S3Service:
  def parseS3Url(s3Url: String): Try[(String, String)] = {
    val s3Pattern = "s3://([^/]+)/(.+)".r
    s3Url match {
      case s3Pattern(bucket, path) => Success((bucket, path))
      case _ => Failure(new IllegalArgumentException("Invalid S3 URL format"))
    }
  }

  def parseS3Urls(urls: Seq[String]): Try[(String, String)] = {
    urls.to(LazyList).map(S3Service.parseS3Url).find(_.isSuccess).getOrElse(Failure(new IllegalStateException("No S3 URL found")))
  }