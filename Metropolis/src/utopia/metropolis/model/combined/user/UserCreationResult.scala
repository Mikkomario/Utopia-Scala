package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible, ModelType, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object UserCreationResult extends FromModelFactory[UserCreationResult]
{
	private val schema = ModelDeclaration("id" -> IntType, "session_key" -> StringType,
		"data" -> ModelType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		UserWithLinks(valid("data").getModel).map { userData =>
			UserCreationResult(valid("id"), userData, valid("session_key"), valid("device_id"), valid("device_key"))
		}
	}
}

/**
  * Wraps information returned by the API when a new user is created
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  */
case class UserCreationResult(userId: Int, userData: UserWithLinks, sessionKey: String, deviceId: Option[Int] = None,
							  deviceKey: Option[String] = None) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> userId, "device_id" -> deviceId,
		"device_key" -> deviceKey, "session_key" -> sessionKey, "data" -> userData.toModel))
}
