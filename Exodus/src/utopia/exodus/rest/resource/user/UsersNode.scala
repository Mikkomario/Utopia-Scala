package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Post
import utopia.access.http.Status._
import utopia.citadel.database.access.many.language.DbLanguages
import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.citadel.database.access.single.user.{DbUser, DbUserSettings}
import utopia.exodus.database.access.many.auth.DbScopes
import utopia.exodus.database.access.single.auth.DbToken
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.model.combined.auth.DetailedToken
import utopia.exodus.model.enumeration.ExodusScope.UserCreation
import utopia.exodus.model.enumeration.ExodusTokenType.{RefreshToken, SessionToken}
import utopia.exodus.model.partial.user.UserPasswordData
import utopia.exodus.rest.resource.user.me.MeNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.exodus.util.{ExodusContext, PasswordHash}
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.combined.user.UserWithLinks
import utopia.metropolis.model.error.{AlreadyUsedException, IllegalPostModelException}
import utopia.metropolis.model.post.NewUser
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success, Try}

object UsersNode extends Resource[AuthorizedContext]
{
	// ATTRIBUTES   ---------------------------
	
	override val name = "users"
	override val allowedMethods = Vector(Post)
	
	
	// IMPLEMENTED	---------------------------
	
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
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(UserCreation) { (token, connection) =>
			implicit val c: Connection = connection
			// Reads user data from the post body
			context.handlePost(NewUser) { userData =>
				// Looks whether user email information has been stored
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
							val scopedToken = token.withScopeLinksPulled
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
								case Some((token, _)) => token -> token.tokenType.refreshedTypeId
								case None => scopedToken -> scopedToken.typeAccess.refreshedTypeId
							}
							val (newSessionToken, sessionTokenString) = DbToken.refreshUsing(parentToken,
								refreshedTokenTypeId.getOrElse { SessionToken.id }, Some(user.id), deviceId,
								ExodusContext.defaultUserScopeIds, modelStylePreference)
							// Attaches scope information to acquired tokens, so that all necessary information may
							// be returned
							val scopeIds = newSessionToken.scopeIds ++ refreshToken.toVector.flatMap { _._1.scopeIds }
							val scopePerId = DbScopes(scopeIds).pull.map { s => s.id -> s }.toMap
							
							// Returns generated user information, along with the new session (and refresh) token
							// (respecting requested styling)
							val style = token.modelStyle
							def tokenToModel(token: DetailedToken, tokenString: String) = {
								val scopes = token.scopeLinks
									.map { link => link.withScopeInfo(scopePerId(link.scopeId)) }
								token.withScopes(scopes).toModelWith(tokenString, style)
							}
							
							val resultModel = user.toModelWith(token.modelStyle) ++
								(Constant("session_token", tokenToModel(newSessionToken, sessionTokenString)) +:
									refreshToken.toVector.map { case (token, tokenString) =>
										Constant("refresh_token", tokenToModel(token, tokenString))
									})
							Result.Success(resultModel, Created)
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
	
	
	// OTHER    --------------------------
	
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