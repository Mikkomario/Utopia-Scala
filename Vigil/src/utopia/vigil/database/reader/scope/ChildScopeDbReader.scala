package utopia.vigil.database.reader.scope

import utopia.vault.nosql.read.linked.CombiningDbRowReader
import utopia.vigil.model.combined.scope.ChildScope
import utopia.vigil.model.stored.scope.{Scope, ScopeRelation}

/**
  * Used for reading child scopes from the database
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
object ChildScopeDbReader 
	extends CombiningDbRowReader[Scope, ScopeRelation, ChildScope](ScopeDbReader, ScopeRelationDbReader)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param scope        scope to wrap
	  * @param linkToParent link to parent to attach
	  */
	override def combine(scope: Scope, linkToParent: ScopeRelation) = ChildScope(scope, linkToParent)
}

