package utopia.metropolis.model.post

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.StringType
import utopia.flow.generic.model.template.ModelConvertible

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
