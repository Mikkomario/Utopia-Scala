package utopia.metropolis.model.partial.description

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

object DescriptionRoleData extends FromModelFactoryWithSchema[DescriptionRoleData]
{
	override val schema = ModelDeclaration("json_key_singular" -> StringType, "json_key_plural" -> StringType)
	
	override protected def fromValidatedModel(model: Model) =
		DescriptionRoleData(model("json_key_singular"), model("json_key_plural"), model("created"))
}

/**
  * An enumeration for different roles or purposes a description can serve
  * @param jsonKeySingular Key used in json documents for a singular value (string) of this description role
  * @param jsonKeyPlural Key used in json documents for multiple values (array) of this description role
  * @param created Time when this DescriptionRole was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class DescriptionRoleData(jsonKeySingular: String, jsonKeyPlural: String, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("json_key_singular" -> jsonKeySingular, "json_key_plural" -> jsonKeyPlural, 
			"created" -> created))
}

