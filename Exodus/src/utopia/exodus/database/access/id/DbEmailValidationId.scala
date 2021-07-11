package utopia.exodus.database.access.id

import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.id.SingleIntIdAccess

/**
  * Used for accessing individual email validation ids
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
object DbEmailValidationId extends SingleIntIdAccess
{
	// IMPLEMENTED	--------------------------
	
	override def table = factory.table
	
	override def target = table
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return The model factory used by this access point
	  */
	def factory = EmailValidationFactory
	
	/**
	  * @return The model interface used in request creation
	  */
	private def model = factory.model
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param key An email validation key
	  * @param connection DB Connection
	  * @return An email validation id matching that key
	  */
	def forValidationKey(key: String)(implicit connection: Connection) =
		find(model.withKey(key).toCondition)
	
	/**
	  * @param key A resend key
	  * @param connection DB Connection (implicit)
	  * @return Id of a valid and open email validation with the specified resend key. None if there is no such
	  *         validation.
	  */
	def forResendKey(key: String)(implicit connection: Connection) =
		find(model.withResendKey(key).toCondition)
}
