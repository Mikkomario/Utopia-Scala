package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object NewDevice extends FromModelFactoryWithSchema[NewDevice]
{
	// IMPLEMENTED	----------------------------
	
	override val schema = ModelDeclaration("name" -> StringType, "language_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model) = NewDevice(model("name"), model("language_id"))
}

/**
  * Used for posting new device data
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  * @param name Name of this new device
  * @param languageId Id of the language used when naming this device
  */
case class NewDevice(name: String, languageId: Int) extends ModelConvertible
{
	override def toModel = Model(Vector("name" -> name, "language_id" -> languageId))
}
