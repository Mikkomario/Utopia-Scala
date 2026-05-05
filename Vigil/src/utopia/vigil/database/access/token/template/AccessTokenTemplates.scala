package utopia.vigil.database.access.token.template

import right.FilterByTokenGrantRight
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vigil.database.reader.token.TokenTemplateDbReader
import utopia.vigil.database.storable.token.TokenGrantRightDbModel
import utopia.vigil.model.stored.token.TokenTemplate

object AccessTokenTemplates 
	extends WrapRowAccess[AccessTokenTemplateRows] with WrapOneToManyAccess[AccessCombinedTokenTemplates] 
		with AccessManyRoot[AccessTokenTemplateRows[TokenTemplate]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(TokenTemplateDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessTokenTemplateRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedTokenTemplates(access)
}

/**
  * Used for accessing multiple token templates from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessTokenTemplates[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessTokenTemplate[A]] with HasValues[AccessTokenTemplateValues] 
		with FilterTokenTemplates[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenTemplateValues(wrapped)
	
	lazy val joinOriginatingGrantRights = join(TokenGrantRightDbModel.grantedTemplateId.column)
	
	lazy val whereOriginatingGrantRight = FilterByTokenGrantRight(joinOriginatingGrantRights)
	
	lazy val joinPossessedTokenGrantRights = join(TokenGrantRightDbModel.ownerTemplateId.column)
	
	lazy val wherePossessedTokenGrantRight = FilterByTokenGrantRight(joinPossessedTokenGrantRights)
}

/**
  * Provides access to row-specific token template -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplateRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessTokenTemplates[A, AccessTokenTemplateRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessTokenTemplateRows[A], AccessTokenTemplate[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessTokenTemplateRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessTokenTemplate(target)
}

/**
  * Used for accessing token template items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedTokenTemplates[A](wrapped: TargetingMany[A]) 
	extends AccessTokenTemplates[A, AccessCombinedTokenTemplates[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedTokenTemplates[A], AccessTokenTemplate[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedTokenTemplates(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessTokenTemplate(target)
}

