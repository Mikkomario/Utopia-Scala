package utopia.vigil.database.access.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}
import utopia.vigil.database.storable.scope.ScopeDbModel

/**
  * Used for accessing scope values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessScopeValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing scope database properties
	  */
	val model = ScopeDbModel
	
	/**
	  * Access to scope ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * A key used for identifying this scope
	  */
	lazy val keys = apply(model.key) { v => v.getString }
	
	/**
	  * ID of the scope that contains this scope. None if this is a root-level scope.
	  */
	lazy val parentIds = apply(model.parentId).flatten { v => v.int }
}

