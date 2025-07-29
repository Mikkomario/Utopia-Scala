package utopia.scribe.api.database.access.logging.issue.variant

import utopia.scribe.api.database.ScribeTables
import utopia.scribe.api.database.access.logging.issue.occurrence.{AccessIssueOccurrenceValue, FilterByIssueOccurrence}
import utopia.scribe.api.database.access.logging.issue.{AccessIssueValue, FilterByIssue}
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessIssueVariant extends AccessOneRoot[AccessIssueVariant[IssueVariant]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessIssueVariants.root.head
	
	/**
	  * Access to individual issue variants in the DB, also including issue information
	  */
	lazy val withIssue = AccessIssueVariants.withIssues.head
	/**
	  * Access to individual issue variants in the DB, also including issue occurrence information
	  */
	lazy val withOccurrence = AccessIssueVariants.withOccurrences.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessIssueVariant[_]): AccessIssueVariantValue = access.values
}

/**
  * Used for accessing individual issue variants from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueVariant[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessIssueVariant[A]] with FilterIssueVariants[AccessIssueVariant[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible issue variant
	  */
	lazy val values = AccessIssueVariantValue(wrapped)
	
	/**
	  * A copy of this access which also targets issue
	  */
	lazy val joinIssue = join(ScribeTables.issue)
	/**
	  * Access to the values of linked issue
	  */
	lazy val issue = AccessIssueValue(joinIssue)
	/**
	  * Access to issue -based filtering functions
	  */
	lazy val whereIssue = FilterByIssue(joinIssue)
	
	/**
	  * A copy of this access which also targets issue_occurrence
	  */
	lazy val joinOccurrence = join(ScribeTables.issueOccurrence)
	/**
	  * Access to the values of linked issue occurrences
	  */
	lazy val occurrence = AccessIssueOccurrenceValue(joinOccurrence)
	/**
	  * Access to issue occurrence -based filtering functions
	  */
	lazy val whereOccurrence = FilterByIssueOccurrence(joinOccurrence)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessIssueVariant(newTarget)
}

