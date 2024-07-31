package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyAuthPreparationsAccess extends ViewFactory[ManyAuthPreparationsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyAuthPreparationsAccess = 
		new _ManyAuthPreparationsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyAuthPreparationsAccess(condition: Condition) extends ManyAuthPreparationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple AuthPreparations at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyAuthPreparationsAccess 
	extends ManyRowModelAccess[AuthPreparation] with Indexed with FilterableView[ManyAuthPreparationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * userIds of the accessible AuthPreparations
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * tokens of the accessible AuthPreparations
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	
	/**
	  * expirationTimes of the accessible AuthPreparations
	  */
	def expirationTimes(implicit connection: Connection) = 
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	
	/**
	  * creationTimes of the accessible AuthPreparations
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationFactory
	
	override protected def self = this
	
	
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyAuthPreparationsAccess = ManyAuthPreparationsAccess(condition)
	
	/**
	  * Updates the created of the targeted AuthPreparation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created, newCreated)
	
	/**
	  * Updates the expires of the targeted AuthPreparation instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the token of the targeted AuthPreparation instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the userId of the targeted AuthPreparation instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

