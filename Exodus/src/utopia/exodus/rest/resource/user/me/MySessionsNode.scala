package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Delete, Post}
import utopia.access.http.Status.{InternalServerError, Unauthorized}
import utopia.exodus.database.access.single.auth.DbToken
import utopia.exodus.model.combined.auth.{DetailedToken, ScopedToken, ScopedTokenLike}
import utopia.exodus.model.enumeration.ExodusTokenType.{RefreshToken, SessionToken}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.post.NewSessionRequest
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for interacting with user sessions
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
object MySessionsNode extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES   ---------------------------
	
	override val name = "sessions"
	override val children = Vector() // TODO: Add
	override val allowedMethods = Vector(Post, Delete)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) = {
		if (context.request.method == Post) {
			???
		}
		else {
			???
		}
	}
	
	
	// OTHER    -------------------------------
	
	private def handlePost()(implicit context: AuthorizedContext) = {
		// May use either basic authorization or a refresh token authorization
		// Case: Authorizing with a refresh token
		if (context.isTokenAuthorized)
			context.authorizedWithoutScope { (token, connection) =>
				implicit val c: Connection = connection
				// The token must be a refresh token of some description
				token.typeAccess.pull match {
					case Some(tokenType) =>
						tokenType.refreshedTypeId match {
							case Some(resultTokenTypeId) =>
								???
							case None =>
								Result.Failure(Unauthorized, "The specified token is not a refresh token")
						}
					case None => Result.Failure(Unauthorized, "The specified token has invalid linking")
				}
			}
		// Case: Authorizing with basic auth
		else
			context.basicAuthorized { (userId, connection) =>
				???
			}
	}
	
	private def startSession(userIdOrParent: Either[Int, DetailedToken])
	                        (implicit context: AuthorizedContext, connection: Connection) =
	{
		// Checks specifications the user may have sent (optional)
		context.handlePossibleValuePost { body =>
			val params = body.model match {
				case Some(postBody) => context.request.parameters ++ postBody
				case None => context.request.parameters
			}
			val sessionRequest = NewSessionRequest.parseFrom(params)
			
			// May generate a new refresh token (only available with basic auth)
			val newRefreshToken = {
				if (sessionRequest.requestRefreshToken)
					userIdOrParent.leftOption.map { userId =>
						DbToken.insert(RefreshToken.id, ownerId = Some(userId),
							scopeIds = ExodusContext.defaultUserScopeIds,
							modelStylePreference = sessionRequest.modelStyle)
					}
				else
					None
			}
			val usedParentToken = newRefreshToken.map { _._1 }.orElse { userIdOrParent.toOption }
			
			// Generates the new session token
			val (sessionToken, sessionTokenString) = usedParentToken match {
				case Some(parentToken) =>
					DbToken.refreshUsing(parentToken, parentToken.tokenType.refreshedTypeId.getOrElse(SessionToken.id),
						customModelStylePreference = sessionRequest.modelStyle,
						customDuration = sessionRequest.customDuration, limitToDefaultDuration = true)
				case None =>
					DbToken.insert(SessionToken.id, None, userIdOrParent.leftOption, ExodusContext.defaultUserScopeIds,
						sessionRequest.modelStyle, sessionRequest.customDuration, limitToDefaultDuration = true)
			}
			
			???
		}
	}
}
