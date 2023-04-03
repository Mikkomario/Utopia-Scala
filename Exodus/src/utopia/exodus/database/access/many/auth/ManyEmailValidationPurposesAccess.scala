package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.database.model.auth.EmailValidationPurposeModel
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyEmailValidationPurposesAccess
{
	// NESTED	--------------------
	
	private class ManyEmailValidationPurposesSubView(override val parent: ManyRowModelAccess[EmailValidationPurpose], 
		override val filterCondition: Condition) 
		extends ManyEmailValidationPurposesAccess with SubView
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
	
	override protected def self = this
	
	override def factory = EmailValidationPurposeFactory
	
	override def filter(additionalCondition: Condition): ManyEmailValidationPurposesAccess = 
		new ManyEmailValidationPurposesAccess.ManyEmailValidationPurposesSubView(this, additionalCondition)
	
	
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

