package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.{IntType, StringType}
import utopia.flow.generic.model.template.ModelConvertible

@deprecated("This class will be removed in a future release", "v2.1")
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
@deprecated("This class will be removed in a future release", "v2.1")
case class NewDevice(name: String, languageId: Int) extends ModelConvertible
{
	override def toModel = Model(Vector("name" -> name, "language_id" -> languageId))
}
