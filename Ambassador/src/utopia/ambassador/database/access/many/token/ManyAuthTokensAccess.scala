package utopia.ambassador.database.access.many.token

import java.time.Instant
import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthTokensAccess
{
	// NESTED	--------------------
	
	private class ManyAuthTokensSubView(override val parent: ManyRowModelAccess[AuthToken], 
		override val filterCondition: Condition) 
		extends ManyAuthTokensAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthTokensAccess extends ManyRowModelAccess[AuthToken] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * userIds of the accessible AuthTokens
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * tokens of the accessible AuthTokens
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	
	/**
	  * expirationTimes of the accessible AuthTokens
	  */
	def expirationTimes(implicit connection: Connection) = 
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	
	/**
	  * creationTimes of the accessible AuthTokens
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * deprecationTimes of the accessible AuthTokens
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { value => value.instant }
	
	/**
	  * areRefreshTokens of the accessible AuthTokens
	  */
	def areRefreshTokens(implicit connection: Connection) = 
		pullColumn(model.isRefreshTokenColumn).flatMap { value => value.boolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyAuthTokensAccess = 
		new ManyAuthTokensAccess.ManyAuthTokensSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the isRefreshToken of the targeted AuthToken instance(s)
	  * @param newIsRefreshToken A new isRefreshToken to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def areRefreshTokens_=(newIsRefreshToken: Boolean)(implicit connection: Connection) = 
		putColumn(model.isRefreshTokenColumn, newIsRefreshToken)
	
	/**
	  * Updates the created of the targeted AuthToken instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the deprecatedAfter of the targeted AuthToken instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the expires of the targeted AuthToken instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the token of the targeted AuthToken instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the userId of the targeted AuthToken instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any AuthToken instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

