package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Post
import utopia.access.http.Status._
import utopia.citadel.database.access.many.language.DbLanguages
import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.citadel.database.access.single.user.{DbUser, DbUserSettings}
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.database.access.single.auth.{DbDeviceToken, DbSessionToken, DbToken}
import utopia.exodus.model.partial.user.UserPasswordData
import utopia.exodus.rest.resource.CustomAuthorizationResourceFactory
import utopia.exodus.rest.resource.user.me.MeNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.{ExodusContext, PasswordHash}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.combined.user.{UserCreationResult, UserWithLinks}
import utopia.metropolis.model.error.{AlreadyUsedException, IllegalPostModelException}
import utopia.metropolis.model.post.NewUser
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success, Try}
import ExodusContext.uuidGenerator
import utopia.exodus.model.combined.auth.DetailedToken
import utopia.exodus.model.enumeration.ExodusScope.UserCreation
import utopia.exodus.model.enumeration.ExodusTokenType
import utopia.exodus.model.enumeration.ExodusTokenType.{RefreshToken, SessionToken}
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}

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
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Inserts a new user to the database using a post model from the request
	  * @param validatePost A function for validating the proposed post model. May modify the model by, for example,
	  *                     inserting a correct email address.
	  * @param context Implicit request context
	  * @param connection Implicit database connection
	  * @return Result to give back to the user. On success, user and session data are sent.
	  */
	def insertUser(validatePost: NewUser => Try[NewUser] = u => Success(u))
	              (wrapSuccess: UserCreationResult => Result = body => Result.Success(body.toModel, Created))
	              (implicit context: AuthorizedContext, connection: Connection) =
	{
		context.handlePost(NewUser) { newUser =>
			validatePost(newUser) match {
				case Success(newUser) =>
					// Saves the new user data to DB
					tryInsert(newUser) match {
						case Success(user) =>
							// Returns a summary of the new data, along with a session key and a possible device key
							val deviceId = user.deviceIds.headOption
							val userId = user.id
							val deviceToken = deviceId.flatMap { deviceId =>
								if (newUser.requestRefreshToken)
									Some(acquireDeviceToken(userId, deviceId))
								else
									None
							}
							val sessionToken = acquireSessionToken(userId, deviceId)
							val creationResult = UserCreationResult(userId, user, sessionToken, deviceId,
								deviceToken)
							wrapSuccess(creationResult)
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
	
	private def handleRequest()(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(UserCreation) { (token, connection) =>
			implicit val c: Connection = connection
			// Reads user data from the post body
			context.handlePost(NewUser) { userData =>
				// Looks whether user email information has been stored
				// TODO: Require an email validation attempt if email validation is enabled
				val preparedEmail = token.access.emailValidationAttempt.emailAddress
				val completeUserData = preparedEmail match {
					case Some(email) => userData.withEmailAddress(email)
					case None => userData
				}
				// Checks whether a required email address is missing
				if (!completeUserData.specifiesEmail && ExodusContext.userEmailIsRequired)
					Result.Failure(BadRequest, "'email' property is missing from the request body")
				else
					// Saves the new user data to DB
					tryInsert(completeUserData) match {
						case Success(user) =>
							// Generates a new refresh token, if requested
							val scopedToken = token.withScopesPulled
							val deviceId = user.deviceIds.headOption
							val modelStylePreference = context.modelStyle
							val refreshToken = {
								if (completeUserData.requestRefreshToken) {
									Some(DbToken.refreshUsing(scopedToken, RefreshToken.id, Some(user.id), deviceId,
										ExodusContext.defaultUserScopeIds, modelStylePreference))
								}
								else
									None
							}
							// Generates a new session token (or other such token)
							val (parentToken, refreshedTokenTypeId) = refreshToken match {
								case Some((token, _)) => token.asScopedToken -> token.tokenType.refreshedTypeId
								case None => scopedToken -> scopedToken.typeAccess.refreshedTypeId
							}
							val (newSessionToken, sessionTokenString) = DbToken.refreshUsing(parentToken,
								refreshedTokenTypeId.getOrElse { SessionToken.id }, Some(user.id), deviceId,
								ExodusContext.defaultUserScopeIds, modelStylePreference)
							
							// Returns generated user information, along with the new session (and refresh) token
							// (respecting requested styling)
							
							token.modelStyle match {
								case Full =>
								case Simple =>
							}
							
							???
						case Failure(error) =>
							error match
							{
								case a: AlreadyUsedException => Result.Failure(Forbidden, a.getMessage)
								case _ => Result.Failure(BadRequest, error.getMessage)
							}
					}
			}
		}
	}
	
	private def acquireDeviceToken(userId: Int, deviceId: Int)(implicit connection: Connection) =
		DbDeviceToken.forDeviceWithId(deviceId).assignToUserWithId(userId).token
	
	private def acquireSessionToken(userId: Int, deviceId: Option[Int])
	                               (implicit connection: Connection, context: AuthorizedContext) =
		DbSessionToken.forSession(userId, deviceId).start(context.modelStyle).token
	
	private def tryInsert(newUser: NewUser)(implicit connection: Connection): Try[UserWithLinks] =
	{
		// Checks whether the proposed email already exist
		val userName = newUser.name.trim
		val email = newUser.email.map { _.trim }.filter { _.nonEmpty }
		
		if (email.exists { !_.contains('@') })
			Failure(new IllegalPostModelException("Email must be a valid email address"))
		else if (userName.isEmpty)
			Failure(new IllegalPostModelException("User name must not be empty"))
		else if (email.exists { DbUserSettings.withEmail(_).nonEmpty })
			Failure(new AlreadyUsedException("Email is already in use"))
		else if ((email.isEmpty || ExodusContext.uniqueUserNamesAreRequired) &&
			DbManyUserSettings.withName(userName).nonEmpty)
			Failure(new AlreadyUsedException("User name is already in use"))
		else
		{
			// Makes sure provided device id or language id matches data in the DB
			val idsAreValid = newUser.device.forall {
				case Right(deviceId) => DbClientDevice(deviceId).nonEmpty
				case Left(newDevice) => DbLanguage(newDevice.languageId).nonEmpty
			}
			if (idsAreValid)
			{
				// Makes sure all the specified languages are also valid
				DbLanguages.validateProposedProficiencies(newUser.languages).flatMap { languages =>
					// Inserts new user data
					val user = DbUser.insert(newUser.name, email,
						languages.map { case (language, familiarity) => language.id -> familiarity.id }.toMap,
						newUser.device)
					// Inserts the new password
					UserPasswordModel.insert(UserPasswordData(user.id, PasswordHash.createHash(newUser.password)))
					// Returns inserted user
					Success(user)
				}
			}
			else
				Failure(new IllegalPostModelException("device_id and language_id must point to existing data"))
		}
	}
}

sealed trait UsersNode extends Resource[AuthorizedContext]
{
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
}

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
		UsersNode.insertUser { newUser =>
			if (isEmailRequired && newUser.email.forall { _.forall { _ == ' ' } })
				Failure(new IllegalPostModelException("'email' must be specified in the request body"))
			else
				Success(newUser)
		}()
	}
}

private class EmailValidatingUsersNode extends UsersNode
{
	// IMPLEMENTED	------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.emailAuthorized(UserCreation.id) { (validationAttempt, connection) =>
			implicit val c: Connection = connection
			// Uses the email that was specified in the validation message
			val result = UsersNode.insertUser { user => Success(user.copy( email = Some(validationAttempt.email) )) }()
			// Closes the email validation if the operation succeeded
			result.isSuccess -> result
		}
	}
}
