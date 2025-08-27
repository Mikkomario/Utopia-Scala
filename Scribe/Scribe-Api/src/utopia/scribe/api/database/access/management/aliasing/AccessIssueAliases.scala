package utopia.scribe.api.database.access.management.aliasing

import utopia.scribe.api.database.reader.management.IssueAliasDbReader
import utopia.scribe.api.database.storable.management.IssueAliasDbModel
import utopia.scribe.core.model.stored.management.IssueAlias
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne

object AccessIssueAliases 
	extends WrapRowAccess[AccessIssueAliasRows] with WrapOneToManyAccess[AccessCombinedIssueAliases] 
		with AccessManyRoot[AccessIssueAliasRows[IssueAlias]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(IssueAliasDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessIssueAliasRows(access)
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedIssueAliases(access)
}

/**
  * Used for accessing multiple issue aliases from the DB at a time
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
abstract class AccessIssueAliases[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessIssueAlias[A]] with HasValues[AccessIssueAliasValues]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessIssueAliasValues(wrapped)
	
	/**
	  * A database model used for interacting with issue alias DB properties
	  */
	val model = IssueAliasDbModel
}

/**
  * Provides access to row-specific issue alias -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessIssueAliasRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessIssueAliases[A, AccessIssueAliasRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessIssueAliasRows[A], AccessIssueAlias[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessIssueAliasRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueAlias(target)
}

/**
  * Used for accessing issue alias items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessCombinedIssueAliases[A](wrapped: TargetingMany[A]) 
	extends AccessIssueAliases[A, AccessCombinedIssueAliases[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedIssueAliases[A], AccessIssueAlias[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedIssueAliases(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueAlias(target)
}

