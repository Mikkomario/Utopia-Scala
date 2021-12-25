package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.database.model.auth.EmailValidationPurposeModel
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.generic.ValueConversions._
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
  * A common trait for access points which target multiple EmailValidationPurposes at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
trait ManyEmailValidationPurposesAccess
	extends ManyRowModelAccess[EmailValidationPurpose] with Indexed
		with FilterableView[ManyEmailValidationPurposesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * englishNames of the accessible EmailValidationPurposes
	  */
	def englishNames(implicit connection: Connection) = 
		pullColumn(model.nameEnColumn).flatMap { value => value.string }
	
	/**
	  * creationTimes of the accessible EmailValidationPurposes
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationPurposeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationPurposeFactory
	
	override def filter(additionalCondition: Condition): ManyEmailValidationPurposesAccess = 
		new ManyEmailValidationPurposesAccess.ManyEmailValidationPurposesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted EmailValidationPurpose instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidationPurpose instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the nameEn of the targeted EmailValidationPurpose instance(s)
	  * @param newNameEn A new nameEn to assign
	  * @return Whether any EmailValidationPurpose instance was affected
	  */
	def englishNames_=(newNameEn: String)(implicit connection: Connection) = 
		putColumn(model.nameEnColumn, newNameEn)
}

