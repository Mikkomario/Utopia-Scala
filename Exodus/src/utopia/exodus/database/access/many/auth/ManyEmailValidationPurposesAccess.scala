package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.database.model.auth.EmailValidationPurposeModel
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyEmailValidationPurposesAccess extends ViewFactory[ManyEmailValidationPurposesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyEmailValidationPurposesAccess = 
		new _ManyEmailValidationPurposesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyEmailValidationPurposesAccess(condition: Condition) 
		extends ManyEmailValidationPurposesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple email validation purposes at a time
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
trait ManyEmailValidationPurposesAccess 
	extends ManyRowModelAccess[EmailValidationPurpose] with FilterableView[ManyEmailValidationPurposesAccess] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * names of the accessible email validation purposes
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn).map { v => v.getString }
	
	/**
	  * creation times of the accessible email validation purposes
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationPurposeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationPurposeFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyEmailValidationPurposesAccess = 
		ManyEmailValidationPurposesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted email validation purposes
	  * @param newCreated A new created to assign
	  * @return Whether any email validation purpose was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the names of the targeted email validation purposes
	  * @param newName A new name to assign
	  * @return Whether any email validation purpose was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}

