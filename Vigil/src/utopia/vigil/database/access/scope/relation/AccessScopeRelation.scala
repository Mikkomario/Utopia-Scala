package utopia.vigil.database.access.scope.relation

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.model.stored.scope.ScopeRelation

object AccessScopeRelation extends AccessOneRoot[AccessScopeRelation[ScopeRelation]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessScopeRelations.root.head
}

/**
  * Used for accessing individual scope relations from the DB at a time
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class AccessScopeRelation[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessScopeRelation[A]] with HasValues[AccessScopeRelationValue] 
		with FilterScopeRelations[AccessScopeRelation[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessScopeRelationValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessScopeRelation(newTarget)
}

