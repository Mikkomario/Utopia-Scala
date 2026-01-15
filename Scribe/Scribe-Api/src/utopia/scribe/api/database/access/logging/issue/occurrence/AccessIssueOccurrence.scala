package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessIssueOccurrence extends AccessOneRoot[AccessIssueOccurrence[IssueOccurrence]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessIssueOccurrences.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessIssueOccurrence[_]): AccessIssueOccurrenceValue = access.values
}

/**
  * Used for accessing individual issue occurrences from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueOccurrence[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessIssueOccurrence[A]] 
		with FilterIssueOccurrences[AccessIssueOccurrence[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible issue occurrence
	  */
	lazy val values = AccessIssueOccurrenceValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessIssueOccurrence(newTarget)
}

