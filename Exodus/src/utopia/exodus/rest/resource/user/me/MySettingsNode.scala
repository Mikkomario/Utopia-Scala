package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Get, Patch, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound, Unauthorized}
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.database.access.single.auth.DbEmailValidationAttempt
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.EmailChange
import utopia.exodus.model.error.InvalidKeyException
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, SessionUseCaseImplementation}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.post.UserSettingsUpdate
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
 * Used for interacting with the authorized user's settings
 * @author Mikko Hilpinen
 * @since 20.5.2020, v1
 */
object MySettingsNode extends ExtendableSessionResource
{
	// ATTRIBUTES   -----------------------
	
	private val defaultGet = SessionUseCaseImplementation.default(Get) { (session, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val ctx: AuthorizedContext = context
		session.userAccess.settings.pull match {
			// Supports simple styling also
			case Some(readSettings) => Result.Success(readSettings.toModelWith(session.modelStyle))
			case None => Result.Failure(NotFound, "No current settings found")
		}
	}
	private val defaultPut = SessionUseCaseImplementation.default(Put) { (session, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cnx: AuthorizedContext = context
		update(session.userId, requireAll = true)
	}
	private val defaultPatch = SessionUseCaseImplementation.default(Patch) { (session, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cnx: AuthorizedContext = context
		update(session.userId)
	}
	
	override val name = "settings"
	override protected val defaultUseCaseImplementations = Vector(defaultGet, defaultPatch, defaultPut)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def defaultFollowImplementations = Vector.empty
	
	
	// OTHER    ---------------------------------------
	
	private def update(userId: Int, requireAll: Boolean = false)
	                  (implicit context: AuthorizedContext, connection: Connection) =
	{
		context.handlePost(UserSettingsUpdate) { update =>
			val settingsAccess = DbUser(userId).settings
			val oldSettings = settingsAccess.pull
			if (oldSettings.forall { update.isPotentiallyDifferentFrom(_) }) {
				// Makes sure name property is provided correctly (if required)
				(if (requireAll) update.name else update.name.orElse { oldSettings.map { _.name } }) match {
					case Some(newName) =>
						// Settings definition regarding email address works differently based on server
						// implementation (whether email validation is used)
						val proposedEmail = {
							// Checks whether email is being changed
							// Case: Is being changed
							if (oldSettings.forall { old => update.definesPotentiallyDifferentEmailThan(old.email) }) {
								// Case: Email validation is used => expects an email validation token in request body
								if (ExodusContext.isEmailValidationSupported)
									// Case: Email token found => completes the validation attempt, if possible
									update.emailValidationToken match {
										case Some(token) =>
											DbEmailValidationAttempt.open
												.completeWithToken(token, EmailChange.id) { validation =>
													// Makes sure this user owns the validation
													if (validation.userId.forall { _ == userId })
														true -> Success(validation.email)
													else
														false -> Failure(new InvalidKeyException(
															"Provided email validation key doesn't belong to you"))
												}.flatten match {
													case Success(email) => Right(Some(email))
													case Failure(error) => Left(Unauthorized -> error.getMessage)
												}
										case None => Right(None)
									}
								// Case: No email validation is used => uses email from the request body
								else
									Right(update.newEmailAddress)
							}
							// Case: Email is not being changed => keeps old email address
							else
								Right(oldSettings.get.email)
						}
						proposedEmail match {
							case Right(email) =>
								// Email address may be required on PUT requests.
								// On PATCH requests, backs up with existing settings.
								if (requireAll && ExodusContext.userEmailIsRequired && email.isEmpty)
									Result.Failure(BadRequest, if (ExodusContext.isEmailValidationSupported)
										"'email_token' is required" else "'email' is required")
								else
								{
									val newEmail = if (requireAll) email else
										email.orElse { oldSettings.flatMap { _.email } }
									settingsAccess.tryUpdate(newName, newEmail,
										ExodusContext.uniqueUserNamesAreRequired) match
									{
										case Success(inserted) => Result.Success(inserted.toModel)
										case Failure(error) => Result.Failure(Forbidden, error.getMessage)
									}
								}
							case Left((status, message)) => Result.Failure(status, message)
						}
					case None => Result.Failure(BadRequest, "Property 'name' is required")
				}
			}
			else
				Result.Success(oldSettings.get.toModel)
		}
	}
}
