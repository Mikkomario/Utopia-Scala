package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object PasswordChange extends FromModelFactoryWithSchema[PasswordChange]
{
	override val schema = ModelDeclaration(PropertyDeclaration("password", StringType))
	
	override protected def fromValidatedModel(model: Model[Constant]) =
		PasswordChange(model("password"), model("device_id"))
}

/**
  * Used for posting password change requests to the server
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  */
case class PasswordChange(newPassword: String, deviceId: Option[Int] = None) extends ModelConvertible
{
	override def toModel = Model(Vector("password" -> newPassword, "device_id" -> deviceId))
}
