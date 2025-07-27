package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.access.many.logging.issue.{AccessIssues, FilterIssues}
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

@deprecated("Replaced with targeting access classes", "v1.2")
object AccessIssue extends AccessOneRoot[AccessIssue[Issue]]
{
	override lazy val root: AccessIssue[Issue] = AccessIssues.root.head
}

/**
  * An interface for accessing individual issues from the DB.
  * Note: The current version is a proof of concept, and not intended for production use.
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.0.5
  */
@deprecated("Replaced with targeting access classes", "v1.2")
case class AccessIssue[A](wrapped: TargetingOne[Option[A]])
	extends AccessOneWrapper[Option[A], AccessIssue[A]] with FilterIssues[AccessIssue[A]]
{
	// ATTRIBUTES   ----------------------------
	
	lazy val values = AccessIssueValue(wrapped)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessIssue(newTarget)
}
