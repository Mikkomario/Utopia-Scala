package utopia.ambassador.model.stored.scope

import utopia.ambassador.model.partial.scope.ScopeData
import utopia.metropolis.model.stored.{StoredModelConvertible, StyledStoredModelConvertible}

/**
  * Represents an access scope stored in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class Scope(id: Int, data: ScopeData) extends StyledStoredModelConvertible[ScopeData]
{
	override protected def includeIdInSimpleModel = true
}
