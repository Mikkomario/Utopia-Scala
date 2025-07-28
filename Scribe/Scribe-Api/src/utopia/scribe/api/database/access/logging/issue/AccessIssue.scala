package utopia.scribe.api.database.access.logging.issue

import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import variant.{AccessIssueVariantValue, FilterByIssueVariant}

import scala.language.implicitConversions

object AccessIssue extends AccessOneRoot[AccessIssue[Issue]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessIssues.root.head
	
	/**
	  * Access to individual issues in the DB, also including issue variant information
	  */
	lazy val withVariants = AccessIssues.withVariants.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessIssue[_]): AccessIssueValue = access.values
}

/**
  * Used for accessing individual issues from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssue[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessIssue[A]] with FilterIssues[AccessIssue[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible issue
	  */
	lazy val values = AccessIssueValue(wrapped)
	
	/**
	  * A copy of this access which also targets issue_variant
	  */
	lazy val joinVariant = join(ScribeTables.issueVariant)
	
	/**
	  * Access to the values of linked issue variants
	  */
	lazy val variant = AccessIssueVariantValue(joinVariant)
	
	/**
	  * Access to issue variant -based filtering functions
	  */
	lazy val whereVariant = FilterByIssueVariant(joinVariant)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessIssue(newTarget)
}

