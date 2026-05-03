package utopia.vigil.database.access.token

import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyDeprecatingRoot, AccessRowsWrapper, AccessWrapper, DeprecatingWrapRowAccess, TargetingMany, TargetingManyLike, TargetingManyRows, TargetingTimeline, WrapOneToManyAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.token.TokenDbReader
import utopia.vigil.database.storable.token.TokenDbModel
import utopia.vigil.model.stored.token.Token

object AccessTokens 
	extends DeprecatingWrapRowAccess[AccessTokenRows](TokenDbModel) 
		with WrapOneToManyAccess[AccessCombinedTokens] with AccessManyDeprecatingRoot[AccessTokenRows[Token]]
{
	// ATTRIBUTES	--------------------
	
	override val all = apply(TokenDbReader).all
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedTokens(access)
	
	override protected def wrap[A](access: TargetingManyRows[A]) = AccessTokenRows(access)
}

/**
  * Used for accessing multiple tokens from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessTokens[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessToken[A]] with HasValues[AccessTokenValues] 
		with FilterTokens[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenValues(wrapped)
}

/**
  * Provides access to row-specific token -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessTokens[A, AccessTokenRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessTokenRows[A], AccessToken[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessTokenRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessToken(target)
}

/**
  * Used for accessing token items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedTokens[A](wrapped: TargetingMany[A]) 
	extends AccessTokens[A, AccessCombinedTokens[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedTokens[A], AccessToken[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedTokens(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessToken(target)
}

