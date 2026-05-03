package utopia.vigil.database.access.token.scope

import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.token.TokenScopeDbReader
import utopia.vigil.model.stored.token.TokenScope

object AccessTokenScopes 
	extends WrapRowAccess[AccessTokenScopeRows] with WrapOneToManyAccess[AccessCombinedTokenScopes] 
		with AccessManyRoot[AccessTokenScopeRows[TokenScope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(TokenScopeDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessTokenScopeRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedTokenScopes(access)
}

/**
  * Used for accessing multiple token scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessTokenScopes[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessTokenScope[A]] with HasValues[AccessTokenScopeValues] 
		with FilterTokenScopes[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenScopeValues(wrapped)
}

/**
  * Provides access to row-specific token scope -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenScopeRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessTokenScopes[A, AccessTokenScopeRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessTokenScopeRows[A], AccessTokenScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessTokenScopeRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessTokenScope(target)
}

/**
  * Used for accessing token scope items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedTokenScopes[A](wrapped: TargetingMany[A]) 
	extends AccessTokenScopes[A, AccessCombinedTokenScopes[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedTokenScopes[A], AccessTokenScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedTokenScopes(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessTokenScope(target)
}

