package utopia.ambassador.model.partial.process

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

/**
  * Links a requested scope to an OAuth preparation
  * @param preparationId Id of the described OAuth preparation
  * @param scopeId Id of the requested scope
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparationScopeLinkData(preparationId: Int, scopeId: Int) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("preparation_id" -> preparationId, "scope_id" -> scopeId))
}

