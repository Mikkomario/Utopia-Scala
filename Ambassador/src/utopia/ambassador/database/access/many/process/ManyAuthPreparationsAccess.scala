package utopia.ambassador.database.access.many.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyAuthPreparationsAccess
{
	// NESTED	--------------------
	
	private class ManyAuthPreparationsSubView(override val parent: ManyRowModelAccess[AuthPreparation], 
		override val filterCondition: Condition) 
		extends ManyAuthPreparationsAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthPreparations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
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
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationFactory
	
	override def filter(additionalCondition: Condition): ManyAuthPreparationsAccess = 
		new ManyAuthPreparationsAccess.ManyAuthPreparationsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthPreparation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
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

