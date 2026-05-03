package utopia.vigil.database.access.token.template.right

import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.token.TokenTemplateScopeDbReader
import utopia.vigil.model.stored.token.TokenTemplateScope

object AccessTokenTemplateScopes 
	extends WrapRowAccess[AccessTokenTemplateScopeRows] 
		with WrapOneToManyAccess[AccessCombinedTokenTemplateScopes] 
		with AccessManyRoot[AccessTokenTemplateScopeRows[TokenTemplateScope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(TokenTemplateScopeDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessTokenTemplateScopeRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedTokenTemplateScopes(access)
}

/**
  * Used for accessing multiple token template scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessTokenTemplateScopes[A, +Repr <: TargetingManyLike[_, Repr, 
	_]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessTokenTemplateScope[A]] 
		with HasValues[AccessTokenTemplateScopeValues] with FilterTokenTemplateScopes[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenTemplateScopeValues(wrapped)
}

/**
  * Provides access to row-specific token template scope -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplateScopeRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessTokenTemplateScopes[A, AccessTokenTemplateScopeRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessTokenTemplateScopeRows[A], AccessTokenTemplateScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessTokenTemplateScopeRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = 
		AccessTokenTemplateScope(target)
}

/**
  * Used for accessing token template scope items that have been combined with one-to-many 
  * combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedTokenTemplateScopes[A](wrapped: TargetingMany[A]) 
	extends AccessTokenTemplateScopes[A, AccessCombinedTokenTemplateScopes[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedTokenTemplateScopes[A], AccessTokenTemplateScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedTokenTemplateScopes(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = 
		AccessTokenTemplateScope(target)
}

