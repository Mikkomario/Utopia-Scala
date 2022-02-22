package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelType, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.StyledModelConvertible

@deprecated("A new version will need to be implemented to support Exodus v4.0 (for Journey)", "v2.0.2")
object UserCreationResult extends FromModelFactory[UserCreationResult]
{
	private val schema = ModelDeclaration("id" -> IntType, "session_token" -> StringType,
		"data" -> ModelType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		UserWithLinks(valid("data").getModel).map { userData =>
			UserCreationResult(valid("id"), userData, valid("session_token"), valid("device_id"), valid("device_token"))
		}
	}
}

/**
  * Wraps information returned by the API when a new user is created
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  */
@deprecated("A new version will need to be implemented to support Exodus v4.0 (for Journey)", "v2.0.2")
case class UserCreationResult(userId: Int, userData: UserWithLinks, sessionToken: String, deviceId: Option[Int] = None,
                              deviceToken: Option[String] = None) extends StyledModelConvertible
{
	override def toModel = Model(Vector("id" -> userId, "device_id" -> deviceId,
		"device_token" -> deviceToken, "session_token" -> sessionToken, "data" -> userData.toModel))
	
	override def toSimpleModel = userData.toSimpleModel ++
		Model(Vector("device_token" -> deviceToken, "session_token" -> sessionToken))
}
