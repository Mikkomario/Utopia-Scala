package utopia.scribe.api.database.access.many.logging.issue

import utopia.scribe.api.database.model.logging.IssueModel
import utopia.vault.nosql.targeting.many.{AccessWrapper, TargetingMany}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

/**
  * An interface for accessing issue data in the DB.
  * NB: Currently this is just a conceptualized interface. Not intended for production use.
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.0.5
  */
trait AccessIssuesLike[A, +Repr <: TargetingMany[A]]
	extends AccessWrapper[A, Repr, TargetingOne[Option[A]]] with FilterIssues[Repr]
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * Model used for interacting with issue DB properties
	  */
	val model = IssueModel
	
	/**
	  * Access to the values of the accessible issues
	  */
	lazy val values = AccessIssueValues(wrapped)
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]): TargetingOne[Option[A]] = target
}
