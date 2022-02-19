package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleScope
import utopia.exodus.model.partial.auth.ScopeData
import utopia.metropolis.model.stored.StyledStoredModelConvertible

/**
  * Represents a scope that has already been stored in the database
  * @param id id of this scope in the database
  * @param data Wrapped scope data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class Scope(id: Int, data: ScopeData) extends StyledStoredModelConvertible[ScopeData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this scope in the database
	  */
	def access = DbSingleScope(id)
	
	
	// IMPLEMENTED  ---------------
	
	override protected def includeIdInSimpleModel = true
}

