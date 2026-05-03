package utopia.vigil.database.access.scope

import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.scope.ScopeDbReader
import utopia.vigil.model.stored.scope.Scope

object AccessScopes 
	extends WrapRowAccess[AccessScopeRows] with WrapOneToManyAccess[AccessCombinedScopes] 
		with AccessManyRoot[AccessScopeRows[Scope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(ScopeDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessScopeRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedScopes(access)
}

/**
  * Used for accessing multiple scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessScopes[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessScope[A]] with HasValues[AccessScopeValues] 
		with FilterScopes[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessScopeValues(wrapped)
}

/**
  * Provides access to row-specific scope -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessScopeRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessScopes[A, AccessScopeRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessScopeRows[A], AccessScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessScopeRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessScope(target)
}

/**
  * Used for accessing scope items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedScopes[A](wrapped: TargetingMany[A]) 
	extends AccessScopes[A, AccessCombinedScopes[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedScopes[A], AccessScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedScopes(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessScope(target)
}

