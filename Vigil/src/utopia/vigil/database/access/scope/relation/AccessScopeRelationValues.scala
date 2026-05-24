package utopia.vigil.database.access.scope.relation

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}
import utopia.vigil.database.storable.scope.ScopeRelationDbModel

/**
  * Used for accessing scope relation values from the DB
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class AccessScopeRelationValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing scope relation database properties
	  */
	val model = ScopeRelationDbModel
	
	/**
	  * Access to scope relation ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * ID of the scope that grants access to another scope
	  */
	lazy val parentScopeIds = apply(model.parentScopeId) { v => v.getInt }
	/**
	  * ID of the scope granted by the linked parent scope
	  */
	lazy val grantedScopeIds = apply(model.grantedScopeId) { v => v.getInt }
	
	
	// COMPUTED -----------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return Each accessible relationship represented with 2 values (as a pair):
	 *              1. ID of the parent scope
	 *              1. ID of the granted child scope
	 */
	def parentAndChildIds(implicit connection: Connection) =
		access.streamColumns(model.parentScopeId, model.grantedScopeId) {
			_.map { _.headPair.map { _.getInt } }.toOptimizedSeq }
}

