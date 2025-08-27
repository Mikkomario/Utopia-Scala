package utopia.scribe.api.database.access.management.comment

import utopia.scribe.core.model.stored.management.Comment
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

object AccessComment extends AccessOneRoot[AccessComment[Comment]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessComments.root.head
}

/**
  * Used for accessing individual comments from the DB at a time
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessComment[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessComment[A]] with HasValues[AccessCommentValue] 
		with FilterComments[AccessComment[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessCommentValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessComment(newTarget)
}

