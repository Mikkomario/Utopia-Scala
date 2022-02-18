package utopia.exodus.model.partial.auth

import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection

/**
  * Tokens used for authenticating requests
  * @param typeId Id of the token type applicable to this token
  * @param hash A hashed version of this token
  * @param ownerId Id of the user who owns this token, if applicable
  * @param deviceId Id of the device this token is tied to, if applicable
  * @param modelStylePreference Model style preferred during this session
  * @param expires Time when this token expires, if applicable
  * @param created Time when this token was issued
  * @param deprecatedAfter Time when this token was revoked or replaced
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenData(typeId: Int, hash: String, ownerId: Option[Int] = None, deviceId: Option[Int] = None, 
	modelStylePreference: Option[ModelStyle] = None, expires: Option[Instant] = None, created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this token has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	
	/**
	  * Whether this token is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	/**
	  * @return An access point to this token's owner's data. None if this token doesn't specify an owner.
	  */
	def userAccess = ownerId.map { DbUser(_) }
	
	/**
	  * @param context Implicit request context
	  * @return The model style to use during a request handling, based on either a header (X-Style) value,
	  * a query parameter (style) value, default from this token or server default
	  */
	def modelStyle(implicit context: AuthorizedContext) =
		context.modelStyle.orElse(modelStylePreference).getOrElse { ExodusContext.defaultModelStyle }
	
	/**
	  * @param context Implicit request context
	  * @param connection Implicit database connection (used for reading language ids from DB if necessary)
	  * @return Language id list to use during this session - from most to least preferred. Empty if no languages
	  *         were specified in the request and this token isn't linked to any user.
	  */
	def languageIds(implicit context: AuthorizedContext, connection: Connection) = ownerId match {
		case Some(userId) => context.languageIds(userId)
		case None => context.requestedLanguageIds
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("type_id" -> typeId, "hash" -> hash, "owner_id" -> ownerId, "device_id" -> deviceId, 
			"model_style_preference" -> modelStylePreference.map { _.id }, "expires" -> expires, 
			"created" -> created, "deprecated_after" -> deprecatedAfter))
}

