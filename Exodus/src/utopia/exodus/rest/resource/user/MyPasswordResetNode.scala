package utopia.exodus.rest.resource.user

import utopia.access.http.Method
import utopia.access.http.Method.Put
import utopia.access.http.Status.{InternalServerError, NotFound}
import utopia.exodus.database.access.id.DbUserId
import utopia.exodus.database.access.single.{DbDevice, DbUser, DbUserSession}
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.PasswordReset
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext._
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.post.PasswordChange
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * This node allows one to reset their password using email validation
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  */
object MyPasswordResetNode extends Resource[AuthorizedContext]
{
	// ATTRIBUTES	-----------------------
	
	override val name = "password"
	
	private val defaultSupportedMethods = Vector[Method](Put)
	
	
	// IMPLEMENTED	-----------------------
	
	override def allowedMethods = if (isEmailValidationSupported) defaultSupportedMethods else Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Authorizes using an email validation
		context.emailAuthorized(PasswordReset.id) { (validation, connection) =>
			// Parses request body
			val result = context.handlePost(PasswordChange) { change =>
				implicit val c: Connection = connection
				validation.ownerId.orElse(DbUserId.forEmail(validation.email)) match
				{
					case Some(userId) =>
						// Updates the user's password in the DB
						if (DbUser(userId).changePassword(change.newPassword))
						{
							// If device id was provided correctly, starts a user session on that device
							change.deviceId.filter { DbDevice(_).isDefined } match
							{
								case Some(deviceId) =>
									val sessionKey = DbUserSession(userId, deviceId).start().key
									Result.Success(sessionKey)
								case None => Result.Empty
							}
						}
						else
							Result.Failure(NotFound, "User data couldn't be found anymore")
					case None => Result.Failure(InternalServerError,
						"Email validation wasn't connected to any user account")
				}
			}
			// Closes email validation on operation success
			result.isSuccess -> result
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
		Error(message = Some(s"$name doesn't have any child nodes"))
}
