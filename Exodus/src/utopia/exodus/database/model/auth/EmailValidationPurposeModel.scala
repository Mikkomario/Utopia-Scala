package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.model.partial.auth.EmailValidationPurposeData
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing EmailValidationPurposeModel instances and for inserting email validation purposes
  *  to the database
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
object EmailValidationPurposeModel 
	extends DataInserter[EmailValidationPurposeModel, EmailValidationPurpose, EmailValidationPurposeData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains email validation purpose name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains email validation purpose created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains email validation purpose name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains email validation purpose created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = EmailValidationPurposeFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: EmailValidationPurposeData) = apply(None, Some(data.name), Some(data.created))
	
	override def complete(id: Value, data: EmailValidationPurposeData) = EmailValidationPurpose(id.getInt, 
		data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this email validation purpose was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A email validation purpose id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param name Name of this email validation purpose. For identification (not localized).
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
}

/**
  * Used for interacting with EmailValidationPurposes in the database
  * @param id email validation purpose database id
  * @param name Name of this email validation purpose. For identification (not localized).
  * @param created Time when this email validation purpose was first created
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
case class EmailValidationPurposeModel(id: Option[Int] = None, name: Option[String] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[EmailValidationPurpose]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationPurposeModel.factory
	
	override def valueProperties = {
		import EmailValidationPurposeModel._
		Vector("id" -> id, nameAttName -> name, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
}

