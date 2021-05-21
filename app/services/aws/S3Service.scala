package services.aws

import play.api.Configuration
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.s3.model.{CopyObjectRequest, GetObjectRequest, HeadObjectRequest, MetadataDirective}
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.net.{URI, URL}
import java.time.Duration
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.ListHasAsScala

@Singleton
class S3Service @Inject()(config: Configuration) {
  val confBuilder: S3Configuration = software.amazon.awssdk.services.s3.S3Configuration.builder()
    .pathStyleAccessEnabled(true)
    .checksumValidationEnabled(false)
    .build()
  val staticCredentialsProvider: StaticCredentialsProvider = StaticCredentialsProvider.create(
    AwsBasicCredentials.create("a99965da-90fe-264f-e7dd-8109bd57823d", "f7c99b73-66d1-da77-982f-0248975105fe")
  )
  private val endpoint = URI.create(config.get[String]("aws.endpoint"))
  val s3: S3Client = S3Client.builder()
    .credentialsProvider(staticCredentialsProvider)
    .endpointOverride(endpoint)
    .serviceConfiguration(confBuilder).build()
  val presigner: S3Presigner = S3Presigner
    .builder()
    .endpointOverride(endpoint)
    .serviceConfiguration(confBuilder)
    .credentialsProvider(staticCredentialsProvider)
    .build();


  def presignedUrl(bucket: String, prefix: String, file: String, expiration: Duration = Duration.ofHours(1)): URL = {

    val key = if (prefix.isEmpty) file else s"$prefix/$file"

    val getObjectRequest =
      GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()

    val getObjectPresignRequest = GetObjectPresignRequest.builder
      .signatureDuration(expiration)
      .getObjectRequest(getObjectRequest).build

    val presignedGetObjectRequest =
      presigner.presignGetObject(getObjectPresignRequest)
    presignedGetObjectRequest.httpRequest.headers.forEach((header, values) => {
      println(s"$header = ${values.asScala}")
    })
    val url = presignedGetObjectRequest.url()
    print(url)
      url
  }


  def copy(bucket: String, prefix: String, file: String): Unit = {
    val key = if (prefix.isEmpty) file else s"$prefix/$file"
    val destkey = if (prefix.isEmpty) file else s"$prefix/${file}_2"
    import java.net.URLEncoder
    import java.nio.charset.StandardCharsets
    val encodedUrl = URLEncoder.encode(bucket + "/" + key, StandardCharsets.UTF_8.toString)
    val cp = CopyObjectRequest.builder()
      .copySource(encodedUrl)
      .contentType("text/plain")
      .contentDisposition("""attachment; filename="my_file.vcf"""")
      .destinationBucket(bucket)
      .destinationKey(destkey)
      .metadataDirective(MetadataDirective.REPLACE)
      .build()
    s3.copyObject(cp)




  }

  def head(bucket: String, prefix: String, file: String): Unit = {
    val key = if (prefix.isEmpty) file else s"$prefix/$file"
    val headRequest = HeadObjectRequest.builder().bucket(bucket).key(key).build()
    val head = s3.headObject(headRequest)
    println(head.contentType())
    println(head.contentDisposition())
    println(head.eTag())
  }

  //
  //  def copy(bucket: String, prefix: String, file: String, expiration: Duration = 1.hour, method: HttpMethod = HttpMethod.GET): URL = {
  //    val expirationDate = new Date(System.currentTimeMillis() + expiration.toMillis)
  //    val key = if (prefix.isEmpty) file else s"$prefix/$file"
  //    CopyObjectRequest.builder()
  //    .
  //    val url = s3.generatePresignedUrl(bucket, s"$prefix/$key", expirationDate, HttpMethod.GET)
  //    print(url)
  //    val l = s3.listObjects(bucket)
  //
  //    url
  //  }
}
