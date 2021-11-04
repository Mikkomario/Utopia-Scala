package utopia.metropolis.model.stored.language

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object Language extends FromModelFactoryWithSchema[Language]
{
	// ATTRIBUTES	-------------------------
	
	override val schema = ModelDeclaration("id" -> IntType, "iso_code" -> StringType)
	
	
	// IMPLEMENTED	------------------------
	
	override protected def fromValidatedModel(model: Model) = Language(model("id"), model("iso_code"))
}

/**
  * Represents a language stored in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class Language(id: Int, isoCode: String) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> id, "iso_code" -> isoCode))
}
