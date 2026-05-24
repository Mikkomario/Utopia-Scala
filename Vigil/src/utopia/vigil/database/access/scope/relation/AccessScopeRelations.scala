package utopia.vigil.database.access.scope.relation

import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.scope.ScopeRelationDbReader
import utopia.vigil.model.stored.scope.ScopeRelation

object AccessScopeRelations 
	extends WrapRowAccess[AccessScopeRelationRows] with WrapOneToManyAccess[AccessCombinedScopeRelations] 
		with AccessManyRoot[AccessScopeRelationRows[ScopeRelation]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(ScopeRelationDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessScopeRelationRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedScopeRelations(access)
}

/**
  * Used for accessing multiple scope relations from the DB at a time
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
abstract class AccessScopeRelations[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessScopeRelation[A]] with HasValues[AccessScopeRelationValues] 
		with FilterScopeRelations[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessScopeRelationValues(wrapped)
}

/**
  * Provides access to row-specific scope relation -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class AccessScopeRelationRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessScopeRelations[A, AccessScopeRelationRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessScopeRelationRows[A], AccessScopeRelation[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessScopeRelationRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessScopeRelation(target)
}

/**
  * Used for accessing scope relation items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class AccessCombinedScopeRelations[A](wrapped: TargetingMany[A]) 
	extends AccessScopeRelations[A, AccessCombinedScopeRelations[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedScopeRelations[A], AccessScopeRelation[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedScopeRelations(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessScopeRelation(target)
}

