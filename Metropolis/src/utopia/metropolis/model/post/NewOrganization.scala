package utopia.metropolis.model.post

import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.{IntType, StringType}
import utopia.flow.generic.model.template.ModelConvertible

object NewOrganization extends FromModelFactoryWithSchema[NewOrganization]
{
	override val schema = ModelDeclaration("name" -> StringType, "language_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model) = NewOrganization(model("name").getString,
		model("language_id").getInt)
}

/**
  * Used for posting new organizations
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class NewOrganization(name: String, languageId: Int) extends ModelConvertible
{
	override def toModel = Model(Vector("name" -> name, "language_id" -> languageId))
}
