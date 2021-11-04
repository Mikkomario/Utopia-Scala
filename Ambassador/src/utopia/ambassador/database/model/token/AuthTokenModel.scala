package utopia.ambassador.database.model.token

import java.time.Instant
import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.SqlExtensions._

/**
  * Used for constructing AuthTokenModel instances and for inserting AuthTokens to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthTokenModel 
	extends DataInserter[AuthTokenModel, AuthToken, AuthTokenData] with Deprecatable
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthToken userId
	  */
	val userIdAttName = "userId"
	/**
	  * Name of the property that contains AuthToken token
	  */
	val tokenAttName = "token"
	/**
	  * Name of the property that contains AuthToken expires
	  */
	val expiresAttName = "expires"
	/**
	  * Name of the property that contains AuthToken created
	  */
	val createdAttName = "created"
	/**
	  * Name of the property that contains AuthToken deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	/**
	  * Name of the property that contains AuthToken isRefreshToken
	  */
	val isRefreshTokenAttName = "isRefreshToken"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthToken userId
	  */
	def userIdColumn = table(userIdAttName)
	/**
	  * Column that contains AuthToken token
	  */
	def tokenColumn = table(tokenAttName)
	/**
	  * Column that contains AuthToken expires
	  */
	def expiresColumn = table(expiresAttName)
	/**
	  * Column that contains AuthToken created
	  */
	def createdColumn = table(createdAttName)
	/**
	  * Column that contains AuthToken deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	/**
	  * Column that contains AuthToken isRefreshToken
	  */
	def isRefreshTokenColumn = table(isRefreshTokenAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthTokenFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthTokenData) = 
		apply(None, Some(data.userId), Some(data.token), data.expires, Some(data.created), 
			data.deprecatedAfter, Some(data.isRefreshToken))
	
	override def complete(id: Value, data: AuthTokenData) = AuthToken(id.getInt, data)
	
	override def nonDeprecatedCondition = deprecatedAfterColumn.isNull && expiresColumn > Now
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this token was acquired / issued
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param deprecatedAfter Time when this token was cancelled, revoked or replaced
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	/**
	  * @param expires Time when this token can no longer be used, if applicable
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	/**
	  * @param id A AuthToken id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param isRefreshToken Whether this is a refresh token which can be used for acquiring access tokens
	  * @return A model containing only the specified isRefreshToken
	  */
	def withIsRefreshToken(isRefreshToken: Boolean) = apply(isRefreshToken = Some(isRefreshToken))
	/**
	  * @param token Textual representation of this token
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
	/**
	  * @param userId Id of the user who owns this token / to whom this token is linked
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with AuthTokens in the database
  * @param id AuthToken database id
  * @param userId Id of the user who owns this token / to whom this token is linked
  * @param token Textual representation of this token
  * @param expires Time when this token can no longer be used, if applicable
  * @param created Time when this token was acquired / issued
  * @param deprecatedAfter Time when this token was cancelled, revoked or replaced
  * @param isRefreshToken Whether this is a refresh token which can be used for acquiring access tokens
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthTokenModel(id: Option[Int] = None, userId: Option[Int] = None, token: Option[String] = None, 
	expires: Option[Instant] = None, created: Option[Instant] = None, 
	deprecatedAfter: Option[Instant] = None, isRefreshToken: Option[Boolean] = None) 
	extends StorableWithFactory[AuthToken]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenModel.factory
	
	override def valueProperties = 
	{
		import AuthTokenModel._
		Vector("id" -> id, userIdAttName -> userId, tokenAttName -> token, expiresAttName -> expires, 
			createdAttName -> created, deprecatedAfterAttName -> deprecatedAfter, 
			isRefreshTokenAttName -> isRefreshToken)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param expires A new expires
	  * @return A new copy of this model with the specified expires
	  */
	def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	/**
	  * @param isRefreshToken A new isRefreshToken
	  * @return A new copy of this model with the specified isRefreshToken
	  */
	def withIsRefreshToken(isRefreshToken: Boolean) = copy(isRefreshToken = Some(isRefreshToken))
	
	/**
	  * @param token A new token
	  * @return A new copy of this model with the specified token
	  */
	def withToken(token: String) = copy(token = Some(token))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}

