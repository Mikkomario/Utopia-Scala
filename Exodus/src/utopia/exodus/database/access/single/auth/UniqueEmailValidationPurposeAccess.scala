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
  * A common trait for access points that return individual and distinct EmailValidationPurposes.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
trait UniqueEmailValidationPurposeAccess 
	extends SingleRowModelAccess[EmailValidationPurpose] 
		with DistinctModelAccess[EmailValidationPurpose, Option[EmailValidationPurpose], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The nameEn of this instance. None if no instance (or value) was found.
	  */
	def nameEn(implicit connection: Connection) = pullColumn(model.nameEnColumn).string
	
	/**
	  * Time when this EmailValidationPurpose was first created. None if no instance (or value) was found.
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
	  * Updates the created of the targeted EmailValidationPurpose instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidationPurpose instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the nameEn of the targeted EmailValidationPurpose instance(s)
	  * @param newNameEn A new nameEn to assign
	  * @return Whether any EmailValidationPurpose instance was affected
	  */
	def nameEn_=(newNameEn: String)(implicit connection: Connection) = putColumn(model.nameEnColumn, 
		newNameEn)
}

