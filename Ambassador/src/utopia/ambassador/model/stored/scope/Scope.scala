package utopia.ambassador.model.stored.scope

import utopia.ambassador.database.access.single.scope.DbSingleScope
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.metropolis.model.stored.StyledStoredModelConvertible

/**
  * Represents a Scope that has already been stored in the database
  * @param id id of this Scope in the database
  * @param data Wrapped Scope data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class Scope(id: Int, data: ScopeData) extends StyledStoredModelConvertible[ScopeData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this Scope in the database
	  */
	def access = DbSingleScope(id)
	
	
	// IMPLEMENTED  ----------------
	
	override protected def includeIdInSimpleModel = true
}

