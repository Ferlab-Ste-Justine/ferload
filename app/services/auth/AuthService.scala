package services.auth

import org.keycloak.TokenVerifier
import org.keycloak.common.VerificationException
import org.keycloak.jose.jws.AlgorithmType
import org.keycloak.representations.AccessToken
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.libs.ws.WSClient

import java.math.BigInteger
import java.security.spec.RSAPublicKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64
import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class AuthService @Inject()(ws: WSClient, config: Configuration, cache: AsyncCacheApi)(implicit ec: ExecutionContext) {

  private val audience = config.get[String]("auth.audience")

  private val issuer = config.get[String]("auth.issuer")

  private val jwksUri = config.get[String]("auth.jwks_uri")

  def verifyToken(token: String): Future[AccessToken] = {
    val tokenVerifier = TokenVerifier.create(token, classOf[AccessToken])
    for {
      publicKey <- fetchPublicKey(tokenVerifier.getHeader.getKeyId)
    } yield publicKey match {
      case Some(pk) =>
        val accessToken = tokenVerifier.publicKey(pk)
          .withChecks(TokenVerifier.IS_ACTIVE, new TokenVerifier.RealmUrlCheck(issuer))
          .audience(audience)
          .verify()
          .getToken
        accessToken

      case None =>
        throw new VerificationException(s"sPublic key ${tokenVerifier.getHeader.getKeyId} not found")
    }
  }

  def fetchPublicKey(key: String): Future[Option[PublicKey]] = {
    cache.getOrElseUpdate(s"auth.public_key.$key", 1.day) {
      ws.url(jwksUri)
        .get()
        .map { r =>
          val keys = (r.json \ "keys").as[Seq[KeyData]]
          keys.collectFirst { case k if k.kid == key => generateKey(k) }
        }
    }
  }

  private def generateKey(keyData: KeyData): PublicKey = {
    val keyFactory = KeyFactory.getInstance(AlgorithmType.RSA.toString)
    val urlDecoder = Base64.getUrlDecoder
    val modulus = new BigInteger(1, urlDecoder.decode(keyData.n))
    val publicExponent = new BigInteger(1, urlDecoder.decode(keyData.e))
    keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent))
  }

}
