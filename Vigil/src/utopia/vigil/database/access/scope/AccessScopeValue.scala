package utopia.vigil.database.access.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vigil.database.storable.scope.ScopeDbModel

/**
  * Used for accessing individual scope values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessScopeValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing scope database properties
	  */
	val model = ScopeDbModel
	
	/**
	  * Access to scope id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * A key used for identifying this scope
	  */
	lazy val key = apply(model.key) { v => v.getString }
	
	/**
	  * ID of the scope that contains this scope. None if this is a root-level scope.
	  */
	lazy val parentId = apply(model.parentId).optional { v => v.int }
}

