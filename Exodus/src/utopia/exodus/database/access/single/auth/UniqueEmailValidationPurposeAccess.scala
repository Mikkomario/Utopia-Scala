package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.database.model.auth.EmailValidationPurposeModel
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct email validation purposes.
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
trait UniqueEmailValidationPurposeAccess 
	extends SingleRowModelAccess[EmailValidationPurpose] 
		with DistinctModelAccess[EmailValidationPurpose, Option[EmailValidationPurpose], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * 
		Name of this email validation purpose. For identification (not localized).. None if no instance (or value)
	  *  was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * Time when this email validation purpose was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationPurposeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationPurposeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted email validation purposes
	  * @param newCreated A new created to assign
	  * @return Whether any email validation purpose was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the names of the targeted email validation purposes
	  * @param newName A new name to assign
	  * @return Whether any email validation purpose was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}

