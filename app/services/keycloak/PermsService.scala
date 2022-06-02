package services.keycloak

import auth.UserRequest
import org.keycloak.authorization.client.util.HttpResponseException
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation
import play.api.Configuration
import play.api.mvc.AnyContent

import java.util
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._

@Singleton
class PermsService @Inject()(config: Configuration, permsClient: PermsClient) {

  val resourcesGlobalName: String = config.get[String]("auth.resources-global-name")

  def checkPermissions(userRequest: UserRequest[AnyContent], files: Set[String]): (Set[String], Set[String]) = {
    try {
      // need to convert the user token into authorization token, because the user token is from a public client
      // and authorization token is from the client with credentials where the resources are declared
      val authorizationToken = if (userRequest.isRpt) userRequest.token
      else permsClient.authzClient.authorization(userRequest.token).authorize().getToken
      val perms = permsClient.authzClient.protection().introspectRequestingPartyToken(authorizationToken)
      val permsNames = Option(perms.getPermissions).getOrElse(new util.ArrayList()).asScala.map(_.getResourceName).toSet
      checkByResourcePermissions(files, permsNames)
    } catch {
      // if 403 user has no permissions at all, return all files as unauthorized
      case e: HttpResponseException => if (e.getStatusCode == 403) (Set(), files) else throw e
      case e: Throwable => throw e
    }
  }

  def checkByResourcePermissions(files: Set[String], permsNames: Set[String]): (Set[String], Set[String]) = {
    if (permsNames.contains(resourcesGlobalName)) {
      (files, Set()) // all authorized
    } else {
      val authorized = files.intersect(permsNames)
      val unauthorized = files.diff(permsNames)
      (authorized, unauthorized)
    }
  }

  def createPermissions(token: String, userName: String, files: Set[String]): (Set[String], Set[String]) = {
    val protection = permsClient.authzClient.protection(token) // insure token has resource creation rights
    val resources = protection.resource()

    val createdPerms = files.flatMap(file => {
      val res = resources.findByName(file)
      if (res != null) { // resource has to exist
        if (!res.getOwnerManagedAccess) { // has to be true
          res.setOwnerManagedAccess(true)
          resources.update(res)
        }
        val policy = protection.policy(res.getId)
        val permName = file
        val existingPerm = policy.find(permName, null, 0, 1) // should be only one perm with this name
          .stream().findFirst
        existingPerm.ifPresentOrElse(perm => {
          if (!perm.getUsers.contains(userName)) {
            perm.addUser(userName)
            policy.update(perm)
          }
        }, () => {
          val perm = new UmaPermissionRepresentation
          perm.setName(permName)
          perm.addUser(userName)
          policy.create(perm)
        })
        Some(file)
      } else {
        None
      }
    })
    (createdPerms, files.diff(createdPerms))
  }
}
