package services

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.{ClientConfiguration, HttpMethod}
import play.api.Configuration

import java.net.URL
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, _}

@Singleton
class S3Service @Inject()(config: Configuration) {
  val clientConfiguration = new ClientConfiguration
  clientConfiguration.setSignerOverride("AWSS3V4SignerType")
  val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.get[String]("aws.access_key"), config.get[String]("aws.secret_key"))))
    .withEndpointConfiguration(new EndpointConfiguration(config.get[String]("aws.endpoint"), Regions.US_EAST_1.name()))
    .withPathStyleAccessEnabled(config.get[Boolean]("aws.path_style_access"))
    .withClientConfiguration(clientConfiguration)
    .build()


  def presignedUrl(bucket: String, key: String, expiration: Duration = 1.hour, method: HttpMethod = HttpMethod.GET): URL = {
    val expirationDate = new Date(System.currentTimeMillis() + expiration.toMillis)

    val url = s3.generatePresignedUrl(bucket, key, expirationDate, HttpMethod.GET)
    url
  }
}
