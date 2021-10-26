package utopia.ambassador.database.access.single.token

import java.time.Instant
import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthTokens.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthTokenAccess 
	extends SingleRowModelAccess[AuthToken] with DistinctModelAccess[AuthToken, Option[AuthToken], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who owns this token / to whom this token is linked. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	
	/**
	  * Textual representation of this token. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	
	/**
	  * Time when this token can no longer be used, if applicable. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Time when this token was acquired / issued. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time when this token was cancelled, revoked or replaced. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	/**
	  * Whether this is a refresh token which can be used for acquiring access tokens. None if no instance (or value) was found.
	  */
	def isRefreshToken(implicit connection: Connection) = pullColumn(model.isRefreshTokenColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthToken instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the deprecatedAfter of the targeted AuthToken instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the expires of the targeted AuthToken instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the isRefreshToken of the targeted AuthToken instance(s)
	  * @param newIsRefreshToken A new isRefreshToken to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def isRefreshToken_=(newIsRefreshToken: Boolean)(implicit connection: Connection) = 
		putColumn(model.isRefreshTokenColumn, newIsRefreshToken)
	
	/**
	  * Updates the token of the targeted AuthToken instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the userId of the targeted AuthToken instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

