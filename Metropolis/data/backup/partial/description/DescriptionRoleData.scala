package utopia.metropolis.model.partial.description

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object DescriptionRoleData extends FromModelFactoryWithSchema[DescriptionRoleData]
{
	override val schema = ModelDeclaration("json_key_singular" -> StringType, "json_key_plural" -> StringType)
	
	override protected def fromValidatedModel(model: Model[Constant]) =
		DescriptionRoleData(model("json_key_singular"), model("json_key_plural"))
}

/**
  * Contains basic information about a description role
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  * @param jsonKeySingular Json property key used for this description role when there are possibly multiple values
  * @param jsonKeyPlural Json property key used for this description role when there is a single defined value
  */
case class DescriptionRoleData(jsonKeySingular: String, jsonKeyPlural: String) extends ModelConvertible
{
	override def toModel = Model(Vector("json_key_singular" -> jsonKeySingular,
		"json_key_plural" -> jsonKeyPlural))
}
