package utopia.metropolis.model.partial.user

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}

object UserSettingsData extends FromModelFactoryWithSchema[UserSettingsData]
{
	override def schema = ModelDeclaration("name" -> StringType, "email" -> StringType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = UserSettingsData(model("name").getString,
		model("email").getString)
}

/**
  * Contains basic data about a user
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  * @param name user-name
  * @param email User's email address
  */
case class UserSettingsData(name: String, email: String) extends ModelConvertible
{
	override def toModel = Model(Vector("name" -> name, "email" -> email))
}
