package utopia.metropolis.model.stored.user

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible, StringType}
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.Stored

object UserSettings extends FromModelFactoryWithSchema[UserSettings]
{
	override val schema = ModelDeclaration("id" -> IntType, "user_id" -> IntType, "name" -> StringType,
		"email" -> StringType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = UserSettings(model("id").getInt,
		model("user_id").getInt, UserSettingsData(model("name").getString, model("email").getString))
}

/**
  * Represents stored user settings
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  */
case class UserSettings(id: Int, userId: Int, data: UserSettingsData) extends Stored[UserSettingsData] with ModelConvertible
{
	override def toModel = Model(Vector("id" -> id, "user_id" -> userId, "name" -> data.name,
		"email" -> data.email))
}
