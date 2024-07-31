package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyAuthRedirectsAccess extends ViewFactory[ManyAuthRedirectsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyAuthRedirectsAccess = 
		 new _ManyAuthRedirectsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyAuthRedirectsAccess(condition: Condition) extends ManyAuthRedirectsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple AuthRedirects at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyAuthRedirectsAccess 
	extends ManyRowModelAccess[AuthRedirect] with Indexed with FilterableView[ManyAuthRedirectsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * preparationIds of the accessible AuthRedirects
	  */
	def preparationIds(implicit connection: Connection) = 
		pullColumn(model.preparationIdColumn).flatMap { value => value.int }
	
	/**
	  * tokens of the accessible AuthRedirects
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	
	/**
	  * expirationTimes of the accessible AuthRedirects
	  */
	def expirationTimes(implicit connection: Connection) = 
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	
	/**
	  * creationTimes of the accessible AuthRedirects
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthRedirectModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthRedirectsAccess = ManyAuthRedirectsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthRedirect instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the expires of the targeted AuthRedirect instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the preparationId of the targeted AuthRedirect instance(s)
	  * @param newPreparationId A new preparationId to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def preparationIds_=(newPreparationId: Int)(implicit connection: Connection) = 
		putColumn(model.preparationIdColumn, newPreparationId)
	
	/**
	  * Updates the token of the targeted AuthRedirect instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
}

