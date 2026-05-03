package utopia.vigil.database.access.scope

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.model.stored.scope.Scope

object AccessScope extends AccessOneRoot[AccessScope[Scope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessScopes.root.head
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

