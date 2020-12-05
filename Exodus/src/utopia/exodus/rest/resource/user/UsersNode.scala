package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Post
import utopia.access.http.Status._
import utopia.exodus.database.access.many.DbUsers
import utopia.exodus.database.access.single.{DbDevice, DbUserSession}
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.UserCreation
import utopia.exodus.rest.resource.CustomAuthorizationResourceFactory
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.handleError
import utopia.exodus.util.ExodusContext
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.combined.user.UserCreationResult
import utopia.metropolis.model.error.{AlreadyUsedException, IllegalPostModelException}
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
			new NoEmailValidationUsersNode(authorize)
	}
}

sealed trait UsersNode extends Resource[AuthorizedContext]
{
	import ExodusContext.uuidGenerator
	
	// IMPLEMENTED	---------------------------
	
	override val name = "users"
	
	override val allowedMethods = Vector(Post)
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "me")
			Follow(MeNode, path.tail)
		else
			Error(message = Some(s"Currently only 'me' is available under $name"))
	}
	
	
	// OTHER	-------------------------------
	
	protected def insertUser(getEmail: NewUser => Try[String])
							(implicit context: AuthorizedContext, connection: Connection) =
	{
		context.handlePost(NewUser) { newUser =>
			getEmail(newUser) match
			{
				case Success(email) =>
					// Saves the new user data to DB
					DbUsers.tryInsert(newUser, email) match
					{
						case Success(userData) =>
							// Returns a summary of the new data, along with a session key and a possible device key
							userData.deviceIds.headOption match
							{
								case Some(deviceId) =>
									val userId = userData.id
									val deviceKey =
									{
										if (newUser.rememberOnDevice)
											Some(acquireDeviceKey(userId, deviceId))
										else
											None
									}
									val sessionKey = acquireSessionKey(userId, deviceId)
									val creationResult = UserCreationResult(userId, deviceId, userData,
										sessionKey, deviceKey)
									Result.Success(creationResult.toModel, Created)
								case None =>
									handleError(new NoSuchElementException(
										"Device id was not available after new user creation"),
										"Device id was not available after new user creation")
									Result.Failure(InternalServerError, "Device id couldn't be acquired")
							}
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
		DbDevice(deviceId).authenticationKey.assignToUserWithId(userId).key
	
	private def acquireSessionKey(userId: Int, deviceId: Int)(implicit connection: Connection) =
		DbUserSession(userId, deviceId).start().key
}

/**
  * A rest-resource for all users
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
private class NoEmailValidationUsersNode(authorize: (AuthorizedContext, Connection => Result) => Response)
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
		// Requires email to be specified in the request body (since its not available as a part of the context)
		insertUser { _.email.toTry { new IllegalPostModelException(
			"'settings' property must be specified in the request body") } }
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
			val result = insertUser { _ => Success(validation.email) }
			// Closes the email validation if the operation succeeded
			result.isSuccess -> result
		}
	}
}
