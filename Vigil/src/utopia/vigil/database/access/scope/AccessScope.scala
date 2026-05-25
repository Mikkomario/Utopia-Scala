package utopia.vigil.database.access.scope

import utopia.flow.collection.immutable.Empty
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.database.access.scope.relation.AccessScopeRelations
import utopia.vigil.model.response.ResponseScope
import utopia.vigil.model.stored.scope.Scope

object AccessScope extends AccessOneRoot[AccessScope[Scope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessScopes.root.head
	
	
	// EXTENSIONS   --------------------
	
	implicit class RichAccessScope(val a: AccessScope[Scope]) extends AnyVal
	{
		/**
		 * Pulls this scope and all granted scopes, converting them to response models
		 * @param connection Implicit DB connection
		 * @return This scope as a response model. None if no scope was accessed.
		 */
		def pullResponseModel(implicit connection: Connection) = {
			a.pull.map { root =>
				val children = AccessScopes.resolveRelationsToResponseModels(
					AccessScopeRelations.withParent(root.id).parentAndChildIds, Set(root.id))
				ResponseScope(root.id, root.key, children.getOrElse(root.id, Empty))
			}
		}
	}
}

/**
  * Used for accessing individual scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessScope[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessScope[A]] with HasValues[AccessScopeValue] 
		with FilterScopes[AccessScope[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessScopeValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessScope(newTarget)
}

