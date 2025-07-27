package utopia.scribe.api.database.access.many.logging.issue

import utopia.scribe.api.database.access.single.logging.issue.AccessIssue
import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessWrapper, TargetingMany}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

@deprecated("Replaced with targeting access classes", "v1.2")
object AccessIssues extends AccessManyRoot[AccessIssues[Issue]]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * Root issue access point
	  */
	override lazy val root = apply(AccessManyRows(IssueFactory))
	
	
	// IMPLICIT ------------------------------
	
	implicit def accessValues(access: AccessIssues[_]): AccessIssueValues = access.values
}

/**
  * An interface for accessing issue data in the DB.
  * NB: Currently this is just a conceptualized interface. Not intended for production use.
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.0.5
  */
@deprecated("Rewritten and moved to another package", "v1.2")
case class AccessIssues[A](wrapped: TargetingMany[A])
	extends AccessWrapper[A, AccessIssues[A], AccessIssue[A]] with FilterIssues[AccessIssues[A]]
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * Access to the values of the accessible issues
	  */
	lazy val values = AccessIssueValues(wrapped)
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def self: AccessIssues[A] = this
	
	override protected def wrap(newTarget: TargetingMany[A]): AccessIssues[A] = AccessIssues(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssue(target)
}
