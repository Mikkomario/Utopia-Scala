package utopia.exodus.rest.resource.user

import utopia.access.http.Method
import utopia.access.http.Method.Put
import utopia.access.http.Status.{BadRequest, InternalServerError, NotFound, Unauthorized}
import utopia.citadel.database.access.id.single.DbUserId
import utopia.citadel.database.access.single.{DbDevice, DbUser}
import utopia.citadel.util.CitadelContext._
import utopia.exodus.database.access.single.{DbEmailValidation, DbUserSession}
import utopia.exodus.database.UserDbExtensions._
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.PasswordReset
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.exodus.util.ExodusContext.handleError
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.post.PasswordChange
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * This node allows one to reset their password using email validation
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  */
object MyPasswordNode extends Resource[AuthorizedContext]
{
	// ATTRIBUTES	-----------------------
	
	override val name = "password"
	
	override val allowedMethods = Vector[Method](Put)
	
	
	// IMPLEMENTED	-----------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// If session (token) authorization is used, changes the password for the logged user.
		// Requires the old password.
		if (context.isTokenAuthorized)
			context.sessionKeyAuthorized { (session, connection) =>
				context.handlePost(PasswordChange) { change =>
					change.oldPassword match
					{
						case Some(oldPassword) =>
							implicit val c: Connection = connection
							// Checks the old password
							val userAccess = DbUser(session.userId)
							if (userAccess.checkPassword(oldPassword))
							{
								// Changes the password
								userAccess.changePassword(change.newPassword)
								// Returns an empty response on success
								Result.Empty
							}
							else
								Result.Failure(Unauthorized, "Old password is incorrect")
						case None => Result.Failure(BadRequest, "Please provide 'old_password' in the request body")
					}
				}
			}
		// If not authorized with a session key / token, expects request body to contain an email authentication
		else
			context.handlePost(PasswordChange) { change =>
				change.emailAuthentication match
				{
					case Some((emailKey, deviceId)) =>
						connectionPool.tryWith { implicit connection =>
							// Checks (and possibly closes) the email validation key
							DbEmailValidation.activateWithKey(emailKey, PasswordReset.id) { validation =>
								// User id is expected to be provided in the validation. Uses email as a backup.
								validation.ownerId.orElse(DbUserId.forEmail(validation.email)) match
								{
									case Some(userId) =>
										// Updates the user's password in the DB
										if (DbUser(userId).changePassword(change.newPassword))
										{
											// If device id was provided correctly, starts a user session on that device
											deviceId.filter { DbDevice(_).isDefined } match
											{
												case Some(deviceId) =>
													val sessionKey = DbUserSession(userId, deviceId).start().key
													true -> Result.Success(sessionKey)
												case None => true -> Result.Empty
											}
										}
										else
											true -> Result.Failure(NotFound, "User data couldn't be found anymore")
									case None => true -> Result.Failure(InternalServerError,
										"Email validation wasn't connected to any user account")
								}
							} match
							{
								case Success(result) => result
								case Failure(error) => Result.Failure(Unauthorized, error.getMessage)
							}
						} match
						{
							case Success(result) => result
							case Failure(error) =>
								handleError(error, "Password recovery failed unexpectedly")
								Result.Failure(InternalServerError, error.getMessage)
						}
					case None => Result.Failure(Unauthorized,
						"Please specify either a session key in headers or 'key' property in request body")
				}
			}.toResponse
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
		Error(message = Some(s"$name doesn't have any child nodes"))
}
