package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.database.model.process.IncompleteAuthLoginModel
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyIncompleteAuthLoginsAccess extends ViewFactory[ManyIncompleteAuthLoginsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyIncompleteAuthLoginsAccess = 
		new _ManyIncompleteAuthLoginsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyIncompleteAuthLoginsAccess(condition: Condition) extends ManyIncompleteAuthLoginsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple IncompleteAuthLogins at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyIncompleteAuthLoginsAccess 
	extends ManyRowModelAccess[IncompleteAuthLogin] with Indexed 
		with FilterableView[ManyIncompleteAuthLoginsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * authIds of the accessible IncompleteAuthLogins
	  */
	def authIds(implicit connection: Connection) = pullColumn(model.authIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * userIds of the accessible IncompleteAuthLogins
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible IncompleteAuthLogins
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * wereSuccesses of the accessible IncompleteAuthLogins
	  */
	def wereSuccesses(implicit connection: Connection) = 
		pullColumn(model.wasSuccessColumn).flatMap { value => value.boolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthLoginModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthLoginFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyIncompleteAuthLoginsAccess = 
		ManyIncompleteAuthLoginsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the authId of the targeted IncompleteAuthLogin instance(s)
	  * @param newAuthId A new authId to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def authIds_=(newAuthId: Int)(implicit connection: Connection) = putColumn(model.authIdColumn, newAuthId)
	
	/**
	  * Updates the created of the targeted IncompleteAuthLogin instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the userId of the targeted IncompleteAuthLogin instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
	
	/**
	  * Updates the wasSuccess of the targeted IncompleteAuthLogin instance(s)
	  * @param newWasSuccess A new wasSuccess to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def wereSuccesses_=(newWasSuccess: Boolean)(implicit connection: Connection) = 
		putColumn(model.wasSuccessColumn, newWasSuccess)
}

