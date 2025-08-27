package utopia.scribe.api.database.access.logging.issue

import utopia.scribe.api.database.ScribeTables
import utopia.scribe.api.database.access.logging.issue.occurrence.{AccessIssueOccurrenceValues, FilterByIssueOccurrence}
import utopia.scribe.api.database.access.logging.issue.variant.{AccessIssueVariantValues, FilterByIssueVariant}
import utopia.scribe.api.database.reader.logging.{IssueDbReader, IssueInstancesDbReader, VaryingIssueDbReader}
import utopia.scribe.api.database.storable.logging.IssueDbModel
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many._
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessIssues 
	extends AccessManyRoot[AccessIssueRows[Issue]] with WrapRowAccess[AccessIssueRows] 
		with WrapOneToManyAccess[AccessCombinedIssues]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(IssueDbReader)
	
	/**
	  * Access to issues in the DB, also including issue variant information
	  */
	lazy val withVariants = apply(VaryingIssueDbReader)
	/**
	  * Access to issues in the DB, including variants and occurrences
	  */
	lazy val withOccurrences = apply(IssueInstancesDbReader)
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessIssues[_, _]): AccessIssueValues = access.values
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingManyRows[A]) = AccessIssueRows(access)
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingMany[A]) = AccessCombinedIssues(access)
}

/**
  * Used for accessing multiple issues from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
abstract class AccessIssues[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessIssue[A]] with FilterIssues[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible issues
	  */
	lazy val values = AccessIssueValues(wrapped)
	
	/**
	  * A copy of this access which also targets issue_variant
	  */
	lazy val joinVariants = join(ScribeTables.issueVariant)
	/**
	  * Access to the values of linked issue variants
	  */
	lazy val variants = AccessIssueVariantValues(joinVariants)
	/**
	  * Access to issue variant -based filtering functions
	  */
	lazy val whereVariants = FilterByIssueVariant(joinVariants)
	
	lazy val joinOccurrences = joinVariants.join(ScribeTables.issueOccurrence)
	lazy val occurrences = AccessIssueOccurrenceValues(joinOccurrences)
	lazy val whereOccurrences = FilterByIssueOccurrence(joinOccurrences)
	
	
	// IMPLEMENTED	--------------------
	
	override def timestampColumn = IssueDbModel.created
}

/**
  * Provides access to row-specific issue -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessIssues[A, AccessIssueRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessIssueRows[A], AccessIssue[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessIssueRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssue(target)
}

/**
  * Used for accessing issue items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessCombinedIssues[A](wrapped: TargetingMany[A]) 
	extends AccessIssues[A, AccessCombinedIssues[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedIssues[A], AccessIssue[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedIssues(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssue(target)
}

