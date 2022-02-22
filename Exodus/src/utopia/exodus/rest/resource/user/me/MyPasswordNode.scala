package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method
import utopia.access.http.Method.Put
import utopia.access.http.Status.Unauthorized
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.database.UserDbExtensions._
import utopia.exodus.model.enumeration.ExodusScope.{ChangeKnownPassword, ReplaceForgottenPassword}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.post.PasswordChange
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * This node allows one to reset their password using email validation
 * @author Mikko Hilpinen
 * @since 3.12.2020, v1
 */
object MyPasswordNode extends LeafResource[AuthorizedContext]
{
	// ATTRIBUTES	-----------------------
	
	override val name = "password"
	override val allowedMethods = Vector[Method](Put)
	
	
	// IMPLEMENTED	-----------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedWithoutScope { (token, connection) =>
			context.handlePost(PasswordChange) { change =>
				// Makes sure the authentication allows password change in this context
				implicit val c: Connection = connection
				if (token.access.hasScope(
					if (change.currentPassword.isDefined) ChangeKnownPassword else ReplaceForgottenPassword)) {
					// Finds the targeted user (either directly linked or linked through an email address)
					token.pullOwnerId match {
						case Some(userId) =>
							// Makes sure the specified old password is valid (if one is provided)
							val passwordAccess = DbUser(userId).password
							if (change.currentPassword.forall { passwordAccess.test(_) }) {
								// Changes the password
								passwordAccess.update(change.newPassword)
								// Returns an empty response on success
								Result.Empty
							}
							else
								Result.Failure(Unauthorized, "Incorrect current password")
						case None => Result.Failure(Unauthorized, "Your current session doesn't specify who you are")
					}
				}
				else if (token.access.hasScope(ChangeKnownPassword))
					Result.Failure(Unauthorized,
						"You must provide 'current_password' within request body in order to continue")
				else
					Result.Failure(Unauthorized, "Your current session doesn't allow password change")
			}
		}
	}
}
