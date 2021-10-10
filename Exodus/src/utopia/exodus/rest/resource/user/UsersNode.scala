package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Post
import utopia.access.http.Status._
import utopia.citadel.database.access.many.device.DbDevices
import utopia.citadel.database.access.many.user.DbUsers
import utopia.citadel.database.access.single.device.DbDevice
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.citadel.database.model.user.{UserDeviceModel, UserLanguageModel, UserModel}
import utopia.exodus.database.access.single.{DbDeviceKey, DbUserSession}
import utopia.exodus.database.model.user.UserAuthModel
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.UserCreation
import utopia.exodus.rest.resource.CustomAuthorizationResourceFactory
import utopia.exodus.rest.resource.user.me.MeNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.datastructure.immutable.Pair
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.combined.user.{UserCreationResult, UserWithLinks}
import utopia.metropolis.model.error.{AlreadyUsedException, IllegalPostModelException}
import utopia.metropolis.model.partial.user.{UserLanguageData, UserSettingsData}
import utopia.metropolis.model.post.NewUser
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success, Try}

object UsersNode extends CustomAuthorizationResourceFactory[UsersNode]
{
	// IMPLEMENTED	------------------------------
	
	// NB: Uses email validation authorization when that is available.
	// Specified authorization function is only used as a backup.
	override def apply(authorize: (AuthorizedContext, Connection => Result) => Response) =
	{
		if (ExodusContext.isEmailValidationSupported)
			new EmailValidatingUsersNode()
		else
			new NoEmailValidationUsersNode(ExodusContext.userEmailIsRequired)(authorize)
	}
}

sealed trait UsersNode extends Resource[AuthorizedContext]
{
	import ExodusContext.uuidGenerator
	
	// IMPLEMENTED	---------------------------
	
	override val name = "users"
	
	override val allowedMethods = Vector(Post)
	
	// Expects /me or /{userId}
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "me")
			Follow(MeNode, path.tail)
		else
			path.head.int match
			{
				case Some(userId) => Follow(OtherUserNode(userId), path.tail)
				case None => Error(message = Some(s"Targeted user id (now '${path.head}') must be an integer"))
			}
	}
	
	
	// OTHER	-------------------------------
	
	// Specified function can return Success(None) if email isn't required
	protected def insertUser(getEmail: NewUser => Try[Option[String]])
							(implicit context: AuthorizedContext, connection: Connection) =
	{
		context.handlePost(NewUser) { newUser =>
			getEmail(newUser) match
			{
				case Success(email) =>
					// Saves the new user data to DB
					tryInsert(newUser, email) match
					{
						case Success(userData) =>
							// Returns a summary of the new data, along with a session key and a possible device key
							val deviceId = userData.deviceIds.headOption
							val userId = userData.id
							val deviceKey = deviceId.flatMap { deviceId =>
								if (newUser.rememberOnDevice)
									Some(acquireDeviceKey(userId, deviceId))
								else
									None
							}
							val sessionKey = acquireSessionKey(userId, deviceId)
							val creationResult = UserCreationResult(userId, userData, sessionKey, deviceId, deviceKey)
							Result.Success(creationResult.toModel, Created)
						case Failure(error) =>
							error match
							{
								case a: AlreadyUsedException => Result.Failure(Forbidden, a.getMessage)
								case _ => Result.Failure(BadRequest, error.getMessage)
							}
					}
				case Failure(error) => Result.Failure(BadRequest, error.getMessage)
			}
		}
	}
	
	private def acquireDeviceKey(userId: Int, deviceId: Int)(implicit connection: Connection) =
		DbDeviceKey.forDeviceWithId(deviceId).assignToUserWithId(userId).key
	
	private def acquireSessionKey(userId: Int, deviceId: Option[Int])
	                             (implicit connection: Connection, context: AuthorizedContext) =
		DbUserSession(userId, deviceId).start(context.modelStyle).key
	
	private def tryInsert(newUser: NewUser, email: Option[String])(implicit connection: Connection): Try[UserWithLinks] =
	{
		// Checks whether the proposed email already exist
		val userName = newUser.userName.trim
		
		if (email.exists { !_.contains('@') })
			Failure(new IllegalPostModelException("Email must be a valid email address"))
		else if (userName.isEmpty)
			Failure(new IllegalPostModelException("User name must not be empty"))
		else if (email.exists { DbUsers.existsUserWithEmail(_) })
			Failure(new AlreadyUsedException("Email is already in use"))
		else if (email.isEmpty && DbUsers.existsUserWithName(newUser.userName))
			Failure(new AlreadyUsedException("User name is already in use"))
		else {
			// Makes sure provided device id or language id matches data in the DB
			val idsAreValid = newUser.device.forall {
				case Right(deviceId) => DbDevice(deviceId).isDefined
				case Left(newDevice) => DbLanguage(newDevice.languageId).isDefined
			}
			
			if (idsAreValid) {
				// Makes sure all the specified languages are also valid
				DbLanguage.validateProposedProficiencies(newUser.languages).flatMap { languages =>
					// Inserts new user data
					val user = UserModel.insert(UserSettingsData(userName, email))
					UserAuthModel.insert(user.id, newUser.password)
					val insertedLanguages = languages.map { case Pair(languageId, familiarity) =>
						UserLanguageModel.insert(UserLanguageData(user.id, languageId, familiarity))
					}
					// Links user with device (if device has been specified) (uses existing or a new device)
					val deviceId = newUser.device.map {
						case Right(deviceId) => deviceId
						case Left(newDevice) => DbDevice.insert(newDevice.name, newDevice.languageId, user.id).targetId
					}
					deviceId.foreach { UserDeviceModel.insert(user.id, _) }
					// Returns inserted user
					Success(UserWithLinks(user, insertedLanguages, deviceId.toVector))
				}
			}
			else
				Failure(new IllegalPostModelException("device_id and language_id must point to existing data"))
		}
	}
}

/**
  * A rest-resource for all users
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
private class NoEmailValidationUsersNode(isEmailRequired: Boolean)
                                        (authorize: (AuthorizedContext, Connection => Result) => Response)
	extends UsersNode
{
	// IMPLEMENTED	---------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Authorizes the request using the specified function
		// Parses and stores new user data if authorization succeeds
		authorize(context, postUser()(context, _))
	}
	
	
	// OTHER	----------------------------
	
	private def postUser()(implicit context: AuthorizedContext, connection: Connection) =
	{
		// May require email to be specified in the request body, based on settings used
		insertUser { newUser =>
			if (isEmailRequired && newUser.email.forall { _.isEmpty })
				Failure(new IllegalPostModelException(
					"'email' must be specified in the request body (under 'settings')"))
			else
				Success(newUser.email)
		}
	}
}

private class EmailValidatingUsersNode extends UsersNode
{
	// IMPLEMENTED	------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.emailAuthorized(UserCreation.id) { (validation, connection) =>
			implicit val c: Connection = connection
			// Uses the email that was specified in the validation message
			val result = insertUser { _ => Success(Some(validation.email)) }
			// Closes the email validation if the operation succeeded
			result.isSuccess -> result
		}
	}
}
