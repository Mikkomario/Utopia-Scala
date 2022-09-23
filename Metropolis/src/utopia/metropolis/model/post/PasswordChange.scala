package utopia.metropolis.model.post

import utopia.flow.collection.value.typeless.{Model, PropertyDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object PasswordChange extends FromModelFactoryWithSchema[PasswordChange]
{
	// ATTRIBUTES   --------------------------
	
	override val schema = ModelDeclaration(PropertyDeclaration("new_password", StringType))
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def fromValidatedModel(model: Model) =
		PasswordChange(model("new_password"), model("current_password", "old_password"))
}

/**
  * Used for posting password change requests to the server
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  * @param newPassword New password to assign
  * @param currentPassword The user's current password (if known)
  */
case class PasswordChange(newPassword: String, currentPassword: Option[String] = None) extends ModelConvertible
{
	// COMPUTED ----------------------------
	
	override def toModel = Model(Vector("new_password" -> newPassword, "current_password" -> currentPassword))
}
