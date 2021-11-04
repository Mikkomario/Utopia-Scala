package utopia.metropolis.model.stored.user

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, StringType}
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.Stored

object UserSettings extends FromModelFactoryWithSchema[UserSettings]
{
	override val schema = ModelDeclaration("id" -> IntType, "user_id" -> IntType, "name" -> StringType)
	
	override protected def fromValidatedModel(model: Model) = UserSettings(model("id").getInt,
		model("user_id").getInt, UserSettingsData(model("name").getString, model("email").string,
			model("created").getInstant))
}

/**
  * Represents stored user settings
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class UserSettings(id: Int, userId: Int, data: UserSettingsData)
	extends Stored[UserSettingsData] with StyledModelConvertible
{
	// IMPLEMENTED  --------------------------
	
	override def toModel = Model(Vector("id" -> id, "user_id" -> userId)) ++ data.toModel
	
	// A simple model omits the user settings version id and uses "id" key for the user id
	override def toSimpleModel = Constant("id", userId) +:
		data.toModel.renamed("created", "last_updated")
}
