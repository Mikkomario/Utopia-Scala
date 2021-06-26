package utopia.exodus.rest.resource.user

import utopia.access.http.Method.{Get, Patch, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound, Unauthorized}
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.database.access.single.DbEmailValidation
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.EmailChange
import utopia.exodus.model.error.InvalidKeyException
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, SessionUseCaseImplementation}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserSettingsData
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
	
	private val defaultGet = SessionUseCaseImplementation.default(Get) { (session, connection, _, _) =>
		implicit val c: Connection = connection
		DbUser(session.userId).settings.pull match
		{
			case Some(readSettings) => Result.Success(readSettings.toModel)
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
		def settings = DbUser(userId).settings
		
		context.handlePost(UserSettingsUpdate) { update =>
			val oldSettings = settings.pull
			if (oldSettings.forall { update.isPotentiallyDifferentFrom(_) })
			{
				// Makes sure name property is provided correctly (if required)
				(if (requireAll) update.name else update.name.orElse { oldSettings.map { _.name } }) match
				{
					case Some(newName) =>
						// Settings definition regarding email address works differently based on server
						// implementation (whether email validation is used)
						val proposedEmail =
						{
							if (oldSettings.forall { old => update.definesPotentiallyDifferentEmailThan(old.email) })
							{
								if (ExodusContext.isEmailValidationSupported)
									update.emailValidationKey match
									{
										case Some(key) =>
											DbEmailValidation.activateWithKey(key, EmailChange.id) { validation =>
												// Makes sure this user owns the validation
												if (validation.ownerId.forall { _ == userId })
													true -> Success(validation.email)
												else
													false -> Failure(new InvalidKeyException(
														"Provided email validation key doesn't belong to you"))
											}.flatten match
											{
												case Success(email) => Right(Some(email))
												case Failure(error) => Left(Unauthorized -> error.getMessage)
											}
										case None => Right(None)
									}
								else
									Right(update.newEmailAddress)
							}
							else
								Right(None)
						}
						proposedEmail match
						{
							case Right(email) =>
								// Email address is required on PUT requests. On PATCH requests,
								// backs up with existing settings.
								(if (requireAll) email else email.orElse { oldSettings.map { _.email } }) match
								{
									case Some(newEmail) =>
										settings.update(UserSettingsData(newName, newEmail)) match
										{
											case Success(inserted) => Result.Success(inserted.toModel)
											case Failure(error) => Result.Failure(Forbidden, error.getMessage)
										}
									case None => Result.Failure(BadRequest,
										if (ExodusContext.isEmailValidationSupported)
											"'email_key' is required" else "'email' is required")
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
