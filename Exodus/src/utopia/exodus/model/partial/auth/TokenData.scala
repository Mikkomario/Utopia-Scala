package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.database.access.single.auth.{DbToken, DbTokenType}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection

/**
  * Tokens used for authenticating requests
  * @param typeId Id of the token type applicable to this token
  * @param hash A hashed version of this token
  * @param parentTokenId Id of the token that was used to acquire this token, if applicable & still known
  * @param ownerId Id of the user who owns this token, if applicable
  * @param modelStylePreference Model style preferred during this session
  * @param expires Time when this token expires, if applicable
  * @param created Time when this token was issued
  * @param deprecatedAfter Time when this token was revoked or replaced
  * @param isSingleUseOnly Whether this token may only be used once (successfully)
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenData(typeId: Int, hash: String, parentTokenId: Option[Int] = None, ownerId: Option[Int] = None,
                     modelStylePreference: Option[ModelStyle] = None, expires: Option[Instant] = None,
	                 created: Instant = Now, deprecatedAfter: Option[Instant] = None, isSingleUseOnly: Boolean = false)
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this token has already been deprecated or expired
	  */
	def isDeprecated = deprecatedAfter.isDefined || expires.exists { Now >= _ }
	/**
	  * Whether this token is still valid (not deprecated nor expired)
	  */
	def isValid = !isDeprecated
	
	/**
	  * @return Whether this token is temporary (will expire at some point)
	  */
	def isTemporary = expires.isDefined
	
	/**
	  * @return An access point to this token's type information in the DB
	  */
	def typeAccess = DbTokenType(typeId)
	/**
	  * An access point to this token's owner's data. None if this token doesn't specify an owner.
	  */
	def userAccess = ownerId.map { DbUser(_) }
	/**
	  * @return An access point to this token's parent token's data. None if this token doesn't have a parent token.
	  */
	def parentAccess = parentTokenId.map { DbToken(_) }
	
	/**
	  * The model style to use during a request handling, based on either a header (X-Style) value,
	  * a query parameter (style) value, default from this token or server default
	  * @param context Implicit request context
	  */
	def modelStyle(implicit context: AuthorizedContext) = 
		context.modelStyle.orElse(modelStylePreference).getOrElse { ExodusContext.defaultModelStyle }
	
	/**
	  * Language id list to use during this session - from most to least preferred. Empty if no languages
	  * were specified in the request and this token isn't linked to any user.
	  * @param context Implicit request context
	  * @param connection Implicit database connection (used for reading language ids from DB if necessary)
	  */
	def languageIds(implicit context: AuthorizedContext, connection: Connection) = {
		ownerId match 
		{
			case Some(userId) => context.languageIds(userId)
			case None => context.requestedLanguageIds
		}
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param tokenString A non-hashed version of this token
	  * @return A model based on this token's information, including the specified non-hashed token
	  */
	def toModelWith(tokenString: String) =
		Model(Vector("token" -> tokenString, "created" -> created, "expires" -> expires,
			"is_single_use_only" -> isSingleUseOnly))
}

