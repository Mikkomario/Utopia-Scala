package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.TokenFactory
import utopia.exodus.database.model.auth.{TokenModel, TokenScopeLinkModel}
import utopia.exodus.model.combined.auth.ScopedTokenLike
import utopia.exodus.model.partial.auth.{TokenData, TokenScopeLinkData}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.util.UuidGenerator
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.Sha256Hasher
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for accessing individual tokens
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbToken extends SingleRowModelAccess[Token] with NonDeprecatedView[Token] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted token
	  * @return An access point to that token
	  */
	def apply(id: Int) = DbSingleToken(id)
	
	/**
	  * Inserts a new token to the database. Allows customization of all token properties.
	  * All specified ids must be valid.
	  * @param typeId Id of the type of this token
	  * @param parentId Id of the token that was used to authorize this token's creation (if applicable)
	  * @param ownerId Id of the owner of this token (if applicable)
	  * @param deviceId Id of the device this token is tied to (if applicable)
	  * @param scopeIds Ids of the scopes accessible using this token (default = empty)
	  * @param duration Duration indicating how long this token is valid, if limited (default = None = not limited)
	  * @param modelStylePreference Preferred model style to use during this session (optional)
	  * @param isSingleUseOnly Whether this token is limited to a single use (default = false)
	  * @param connection Implicit DB Connection
	  * @param uuidGenerator Implicit UUID generator
	  * @return A new token, along with the non-hashed token string
	  */
	def insertCustom(typeId: Int, parentId: Option[Int] = None, ownerId: Option[Int] = None,
	                 deviceId: Option[Int] = None, duration: Option[FiniteDuration] = None, scopeIds: Set[Int] = Set(),
	                 modelStylePreference: Option[ModelStyle] = None, isSingleUseOnly: Boolean = false)
	                (implicit connection: Connection, uuidGenerator: UuidGenerator) =
	{
		// Generates the new token string
		val tokenString = uuidGenerator.next()
		// Stores the token into the database
		val insertedToken = model.insert(TokenData(typeId, Sha256Hasher(tokenString), parentId, ownerId,
			deviceId, modelStylePreference, duration.map { Now + _ }, isSingleUseOnly = isSingleUseOnly))
		// Grants scopes (according to parent token scopes & custom extra scopes)
		val scopeLinks = TokenScopeLinkModel.insert(
			scopeIds.toVector.sorted.map { scopeId => TokenScopeLinkData(insertedToken.id, scopeId) })
		
		// Returns the token in a detailed form. Also includes the non-hashed token string.
		insertedToken.withScopes(scopeLinks) -> tokenString
	}
	
	/**
	  * Inserts a new token to the database. All specified ids must be valid.
	  * @param typeId Id of the type of this token
	  * @param parentId Id of the token that was used to authorize this token's creation (if applicable)
	  * @param ownerId Id of the owner of this token (if applicable)
	  * @param deviceId Id of the device this token is tied to (if applicable)
	  * @param scopeIds Ids of the scopes accessible using this token (default = empty)
	  * @param modelStylePreference Preferred model style to use during this session (optional)
	  * @param customDuration Duration to overwrite the default duration with (optional)
	  * @param limitToDefaultDuration Whether this token type's default duration should be used as a maximum value,
	  *                               even when a custom duration has been specified (default = false).
	  * @param connection Implicit DB Connection
	  * @param uuidGenerator Implicit UUID generator
	  * @return A new token, along with the non-hashed token string
	  */
	def insert(typeId: Int, parentId: Option[Int] = None, ownerId: Option[Int] = None, deviceId: Option[Int] = None,
	           scopeIds: Set[Int] = Set(), modelStylePreference: Option[ModelStyle] = None,
	           customDuration: Option[Duration] = None, limitToDefaultDuration: Boolean = false)
	          (implicit connection: Connection, uuidGenerator: UuidGenerator) =
	{
		// Reads token type information (not expected to fail)
		val tokenType = DbTokenType(typeId).pull
			.toTry { new NoSuchElementException(s"Token type id $typeId is not valid") }.get
		val duration = customDuration match {
			case Some(custom) =>
				if (limitToDefaultDuration && tokenType.duration.exists { custom > _ })
					tokenType.duration
				else
					custom.finite
			case None => tokenType.duration
		}
		// Inserts the new token
		val (token, tokenString) = insertCustom(typeId, parentId, ownerId, deviceId, duration, scopeIds,
			modelStylePreference, tokenType.isSingleUseOnly)
		// Returns the token in a detailed form. Also includes the non-hashed token string.
		token.withTypeInfo(tokenType) -> tokenString
	}
	
	/**
	  * Creates a new token by using a parent token as the basis. All specified ids must be valid.
	  * @param parentToken Parent token (i.e. token used to authorize this token's creation)
	  * @param newTypeId Id of this new token's type
	  * @param ownerIdLimit Id of the new owner of this token, if different from the parent token's owner
	  * @param deviceIdLimit Id of the device this token is linked with, if different from the parent token's linking
	  * @param additionalScopeIds Scopes to grant in addition to those granted by the parent token (default = empty)
	  * @param customModelStylePreference Model style preference to overwrite that of the parent token (optional)
	  * @param customDuration Duration to overwrite the default duration with (optional)
	  * @param limitToDefaultDuration Whether this token type's default duration should be used as a maximum value,
	  *                               even when a custom duration has been specified (default = false).
	  * @param connection Implicit DB Connection
	  * @param uuidGenerator Implicit UUID generator
	  * @return The generated token, along with the new non-hashed token string
	  */
	def refreshUsing(parentToken: ScopedTokenLike, newTypeId: Int, ownerIdLimit: Option[Int] = None,
	                 deviceIdLimit: Option[Int] = None, additionalScopeIds: Set[Int] = Set(),
	                 customModelStylePreference: Option[ModelStyle] = None,
	                 customDuration: Option[Duration] = None, limitToDefaultDuration: Boolean = false)
	                (implicit connection: Connection, uuidGenerator: UuidGenerator) =
		insert(newTypeId, Some(parentToken.id), ownerIdLimit.orElse { parentToken.ownerId },
			deviceIdLimit.orElse { parentToken.deviceId }, parentToken.scopeIds ++ additionalScopeIds,
			customModelStylePreference.orElse { parentToken.modelStylePreference }, customDuration,
			limitToDefaultDuration)
	
	/**
	  * @param token A token string (not hashed)
	  * @return An access point to a matching valid token
	  */
	def matching(token: String) = new DbTokenMatch(token)
	
	
	// NESTED	--------------------
	
	class DbTokenMatch(tokenString: String) extends UniqueTokenAccess with SubView
	{
		// ATTRIBUTES	--------------------
		
		// Tokens are hashed using SHA256 algorithm
		override lazy val filterCondition = model.withHash(Sha256Hasher(tokenString)).toCondition
		
		
		// IMPLEMENTED	--------------------
		
		override protected def parent = DbToken
	}
}

