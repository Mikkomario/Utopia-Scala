package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.model.partial.auth.EmailValidationPurposeData
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing EmailValidationPurposeModel instances and for inserting EmailValidationPurposes to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object EmailValidationPurposeModel 
	extends DataInserter[EmailValidationPurposeModel, EmailValidationPurpose, EmailValidationPurposeData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains EmailValidationPurpose nameEn
	  */
	val nameEnAttName = "nameEn"
	
	/**
	  * Name of the property that contains EmailValidationPurpose created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains EmailValidationPurpose nameEn
	  */
	def nameEnColumn = table(nameEnAttName)
	
	/**
	  * Column that contains EmailValidationPurpose created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = EmailValidationPurposeFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: EmailValidationPurposeData) = apply(None, Some(data.nameEn), Some(data.created))
	
	override def complete(id: Value, data: EmailValidationPurposeData) = EmailValidationPurpose(id.getInt, 
		data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this EmailValidationPurpose was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A EmailValidationPurpose id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @return A model containing only the specified nameEn
	  */
	def withNameEn(nameEn: String) = apply(nameEn = Some(nameEn))
}

/**
  * Used for interacting with EmailValidationPurposes in the database
  * @param id EmailValidationPurpose database id
  * @param created Time when this EmailValidationPurpose was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class EmailValidationPurposeModel(id: Option[Int] = None, nameEn: Option[String] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[EmailValidationPurpose]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationPurposeModel.factory
	
	override def valueProperties = 
	{
		import EmailValidationPurposeModel._
		Vector("id" -> id, nameEnAttName -> nameEn, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param nameEn A new nameEn
	  * @return A new copy of this model with the specified nameEn
	  */
	def withNameEn(nameEn: String) = copy(nameEn = Some(nameEn))
}

