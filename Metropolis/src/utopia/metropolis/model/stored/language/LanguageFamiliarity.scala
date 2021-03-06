package utopia.metropolis.model.stored.language

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object LanguageFamiliarity extends FromModelFactoryWithSchema[LanguageFamiliarity]
{
	override val schema = ModelDeclaration("id" -> IntType, "order_index" -> IntType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = LanguageFamiliarity(model("id"),
		model("order_index"))
}

/**
  * Represents a recorded language familiarity level
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
case class LanguageFamiliarity(id: Int, orderIndex: Int) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> id, "order_index" -> orderIndex))
}
