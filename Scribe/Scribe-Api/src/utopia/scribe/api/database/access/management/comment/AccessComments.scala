package utopia.scribe.api.database.access.management.comment

import utopia.scribe.api.database.reader.management.CommentDbReader
import utopia.scribe.core.model.stored.management.Comment
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, TargetingTimeline, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne

object AccessComments 
	extends WrapRowAccess[AccessCommentRows] with WrapOneToManyAccess[AccessCombinedComments] 
		with AccessManyRoot[AccessCommentRows[Comment]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(CommentDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessCommentRows(access)
	override def apply[A](access: TargetingMany[A]) = AccessCombinedComments(access)
}

/**
  * Used for accessing multiple comments from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
abstract class AccessComments[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessComment[A]] with HasValues[AccessCommentValues] 
		with FilterComments[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessCommentValues(wrapped)
}

/**
  * Provides access to row-specific comment -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class AccessCommentRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessComments[A, AccessCommentRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessCommentRows[A], AccessComment[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessCommentRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessComment(target)
}

/**
  * Used for accessing comment items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class AccessCombinedComments[A](wrapped: TargetingMany[A]) 
	extends AccessComments[A, AccessCombinedComments[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedComments[A], AccessComment[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedComments(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessComment(target)
}

