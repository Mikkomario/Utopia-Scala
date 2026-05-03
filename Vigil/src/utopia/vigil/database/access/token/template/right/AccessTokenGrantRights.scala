package utopia.vigil.database.access.token.template.right

import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.token.TokenGrantRightDbReader
import utopia.vigil.model.stored.token.TokenGrantRight

object AccessTokenGrantRights 
	extends WrapRowAccess[AccessTokenGrantRightRows] with WrapOneToManyAccess[AccessCombinedTokenGrantRights] 
		with AccessManyRoot[AccessTokenGrantRightRows[TokenGrantRight]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(TokenGrantRightDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessTokenGrantRightRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedTokenGrantRights(access)
}

/**
  * Used for accessing multiple token grant rights from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessTokenGrantRights[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessTokenGrantRight[A]] with HasValues[AccessTokenGrantRightValues] 
		with FilterTokenGrantRights[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenGrantRightValues(wrapped)
}

/**
  * Provides access to row-specific token grant right -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenGrantRightRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessTokenGrantRights[A, AccessTokenGrantRightRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessTokenGrantRightRows[A], AccessTokenGrantRight[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessTokenGrantRightRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessTokenGrantRight(target)
}

/**
  * Used for accessing token grant right items that have been combined with one-to-many 
  * combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedTokenGrantRights[A](wrapped: TargetingMany[A]) 
	extends AccessTokenGrantRights[A, AccessCombinedTokenGrantRights[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedTokenGrantRights[A], AccessTokenGrantRight[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedTokenGrantRights(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessTokenGrantRight(target)
}

