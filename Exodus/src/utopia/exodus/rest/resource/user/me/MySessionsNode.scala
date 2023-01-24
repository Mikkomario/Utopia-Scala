package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Delete, Post}
import utopia.access.http.Status.Unauthorized
import utopia.exodus.database.access.many.auth.{DbScopes, DbTokens, DbTypedTokens}
import utopia.exodus.database.access.single.auth.DbToken
import utopia.exodus.model.combined.auth.DetailedToken
import utopia.exodus.model.enumeration.ExodusScope.TerminateOtherSessions
import utopia.exodus.model.enumeration.ExodusTokenType.{RefreshToken, SessionToken}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.collection.CollectionExtensions._
import utopia.metropolis.model.post.NewSessionRequest
import utopia.nexus.http.Path
import utopia.nexus.rest.{LeafResource, Resource, ResourceWithChildren}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for interacting with user sessions. Intended to appear under users/me
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
object MySessionsNode extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES   ---------------------------
	
	override val name = "sessions"
	override val children = Vector[Resource[AuthorizedContext]](
		CurrentSessionNode, PreviousSessionsNode, OtherSessionsNode)
	override val allowedMethods = Vector(Post, Delete)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) = {
		// Case: POST => Starts a new session
		if (context.request.method == Post)
			handlePost()
		// Case: DELETE => ends all linked sessions
		else
			handleDeleteAll()
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
						if (tokenType.isRefreshToken) {
							// Loads scope link information, also
							startSession(Right(token.withScopeLinksPulled.withTypeInfo(tokenType)))
						}
						else
							Result.Failure(Unauthorized, "The specified token is not a refresh token")
					case None => Result.Failure(Unauthorized, "The specified token has invalid linking")
				}
			}
		// Case: Authorizing with basic auth
		else
			context.basicAuthorized { (userId, connection) =>
				implicit val c: Connection = connection
				startSession(Left(userId))
			}
	}
	
	private def handleDeleteAll()(implicit context: AuthorizedContext) =
		handleDelete(includeOther = true, includePrevious = true, includeCurrent = true)
	
	// includeOther: Whether other owned sessions should be ended
	// includePrevious: Whether this session's children or siblings should be ended
	// includeCurrent: Whether this session should be ended
	private def handleDelete(includeOther: Boolean = false, includePrevious: Boolean = false,
	                          includeCurrent: Boolean = false)
	                         (implicit context: AuthorizedContext) =
	{
		lazy val temporaryAccessTokensAccess = DbTokens.temporary.withTypeInfo.accessTokens
		def deprecateUsersSessions(userId: Int, excludeTokenId: Option[Int] = None)
		                          (implicit connection: Connection) =
		{
			val base = temporaryAccessTokensAccess.ownedByUserWithId(userId)
			val target = excludeTokenId match {
				case Some(excludedId) => base.excludingTokenWithId(excludedId)
				case None => base
			}
			target.deprecate()
		}
		
		// Uses either token-based auth (session- or a refresh token) or basic auth
		if (context.isTokenAuthorized) {
			def onceAuthorized(token: Token, connection: Connection) =
			{
				implicit val c: Connection = connection
				// May revoke:
				// a) This session (unless this is a refresh token or an infinite access token)
				if (includeCurrent && token.isTemporary && token.typeAccess.isAccessToken)
					token.access.deprecate()
				// b) All sessions created using this session
				if (includeCurrent || includePrevious) {
					val childTokenIds = findTokenIdsUnder(Set(token.id))
					if (childTokenIds.nonEmpty)
						DbTypedTokens(childTokenIds).accessTokens.deprecate()
				}
				// c) All temporary non-refresh sessions owned by this user (if applicable)
				if (includeOther)
					token.ownerId.foreach { deprecateUsersSessions(_, if (includeCurrent) None else Some(token.id)) }
				// d) Sessions created using the same parent token (if both are owned by this user)
				else if (includePrevious)
					token.parentTokenId
						.filter { parentId =>
							token.ownerId.exists { ownerId => DbToken(parentId).isOwnedByUserWithId(ownerId) }
						}
						.foreach { parentId =>
							val base = temporaryAccessTokensAccess.createdUsingTokenWithId(parentId)
							val target = if (includeCurrent) base else base.excludingTokenWithId(token.id)
							target.deprecate()
						}
				
				Result.Empty
			}
			// May require a specific scope
			if (includeOther)
				context.authorizedForScope(TerminateOtherSessions)(onceAuthorized)
			else
				context.authorizedWithoutScope(onceAuthorized)
		}
		else
			context.basicAuthorized { (userId, connection) =>
				// Revokes all sessions owned by this user (if requested)
				if (includeOther)
					deprecateUsersSessions(userId)(connection)
				Result.Empty
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
			
			// Ends previous sessions, if that was requested
			if (sessionRequest.revokePrevious)
				endPreviousSessions(userIdOrParent.mapRight { _.id })
			
			// May generate a new refresh token (only available with basic auth)
			val newRefreshToken = {
				if (sessionRequest.requestRefreshToken)
					userIdOrParent.leftOption.map { userId =>
						DbToken.insert(RefreshToken.id, ownerId = Some(userId),
							forwardedScopeIds = ExodusContext.defaultUserScopeIds,
							modelStylePreference = sessionRequest.modelStyle)
					}
				else
					None
			}
			val usedParentToken = newRefreshToken.map { _._1 }.orElse { userIdOrParent.toOption }
			
			// Generates the new session token
			val (newSessionToken, sessionTokenString) = usedParentToken match {
				case Some(parentToken) =>
					DbToken.refreshUsing(parentToken, parentToken.tokenType.refreshedTypeId.getOrElse(SessionToken.id),
						customModelStylePreference = sessionRequest.modelStyle,
						customDuration = sessionRequest.customDuration, limitToDefaultDuration = true)
				case None =>
					DbToken.insert(SessionToken.id, None, userIdOrParent.leftOption, ExodusContext.defaultUserScopeIds,
						Set(), sessionRequest.modelStyle, sessionRequest.customDuration, limitToDefaultDuration = true)
			}
			
			// Reads scope information so that it may be included in the response
			val scopePerId = DbScopes(newSessionToken.allScopeIds ++
				newRefreshToken.iterator.flatMap { _._1.allScopeIds })
				.pull.map { s => s.id -> s }.toMap
			
			// Forms the response
			val style = sessionRequest.modelStyle
				.orElse { context.modelStyle }
				.orElse { userIdOrParent.toOption.flatMap { _.modelStylePreference } }
				.getOrElse { ExodusContext.defaultModelStyle }
			val sessionTokenModel = newSessionToken.withScopeInfo(scopePerId).toModelWith(sessionTokenString, style)
			val resultModel = newRefreshToken match {
				case Some((refreshToken, refreshTokenString)) =>
					sessionTokenModel +Constant("refresh_token",
						refreshToken.withScopeInfo(scopePerId).toModelWith(refreshTokenString, style))
				case None => sessionTokenModel
			}
			Result.Success(resultModel)
		}
	}
	
	// userOrParentTokenId: Left is userId, Right is parent token id
	private def endPreviousSessions(userOrParentTokenId: Either[Int, Int], currentTokenId: Option[Int] = None)
	                               (implicit connection: Connection) =
	{
		val baseAccess = userOrParentTokenId match {
			case Right(parentTokenId) => DbTokens.createdUsingTokenWithId(parentTokenId)
			case Left(userId) => DbTokens.ownedByUserWithId(userId)
		}
		val targetAccess = currentTokenId match {
			case Some(currentTokenId) => baseAccess.excludingTokenWithId(currentTokenId)
			case None => baseAccess
		}
		targetAccess.deprecate()
	}
	
	// Finds all token ids under the specified parent token ids (directly or indirectly).
	// Only targets active tokens
	private def findTokenIdsUnder(parentTokenIds: Set[Int])(implicit connection: Connection): Set[Int] =
	{
		if (parentTokenIds.isEmpty)
			Set[Int]()
		else {
			val childTokenIds = {
				if (parentTokenIds.hasSize(1))
					DbTokens.createdUsingTokenWithId(parentTokenIds.head).ids.toSet
				else
					DbTokens.createdUsingAnyOfTokens(parentTokenIds).ids.toSet
			}
			val newTokenIds = childTokenIds -- parentTokenIds
			if (newTokenIds.isEmpty)
				childTokenIds
			else {
				// Continues recursively until no results are found anymore
				childTokenIds ++ findTokenIdsUnder(newTokenIds)
			}
		}
	}
	
	
	// NESTED   --------------------------
	
	private object CurrentSessionNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES   ------------------
		
		override val name = "current"
		override val allowedMethods = Vector(Delete)
		
		
		// IMPLEMENTED  ------------------
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
			handleDelete(includeCurrent = true)
	}
	
	private object OtherSessionsNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES   ------------------
		
		override val name = "other"
		override val allowedMethods = Vector(Delete)
		
		
		// IMPLEMENTED  ------------------
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
			handleDelete(includeOther = true)
	}
	
	private object PreviousSessionsNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES   ------------------
		
		override val name = "previous"
		override val allowedMethods = Vector(Delete)
		
		
		// IMPLEMENTED  ------------------
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
			handleDelete(includePrevious = true)
	}
}
