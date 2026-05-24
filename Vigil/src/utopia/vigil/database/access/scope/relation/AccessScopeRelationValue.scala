package utopia.vigil.database.access.scope.relation

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vigil.database.storable.scope.ScopeRelationDbModel

/**
  * Used for accessing individual scope relation values from the DB
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class AccessScopeRelationValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing scope relation database properties
	  */
	val model = ScopeRelationDbModel
	
	/**
	  * Access to scope relation id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * ID of the scope that grants access to another scope
	  */
	lazy val parentScopeId = apply(model.parentScopeId).optional { v => v.int }
	
	/**
	  * ID of the scope granted by the linked parent scope
	  */
	lazy val grantedScopeId = apply(model.grantedScopeId).optional { v => v.int }
}

