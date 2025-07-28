package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.scribe.api.database.ScribeTables
import utopia.scribe.api.database.access.logging.issue.variant.{AccessIssueVariantValues, FilterByIssueVariant}
import utopia.scribe.api.database.access.logging.issue.{AccessIssueValues, FilterByIssue}
import utopia.scribe.api.database.reader.logging.IssueOccurrenceDbReader
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many._
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessIssueOccurrences 
	extends AccessManyRoot[AccessIssueOccurrenceRows[IssueOccurrence]] 
		with WrapRowAccess[AccessIssueOccurrenceRows] with WrapOneToManyAccess[AccessCombinedIssueOccurrences]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(IssueOccurrenceDbReader)
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessIssueOccurrences[_, _]): AccessIssueOccurrenceValues = access.values
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingManyRows[A]) = AccessIssueOccurrenceRows(access)
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingMany[A]) = AccessCombinedIssueOccurrences(access)
}

/**
  * Used for accessing multiple issue occurrences from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
abstract class AccessIssueOccurrences[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessIssueOccurrence[A]] with FilterIssueOccurrences[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible issue occurrences
	  */
	lazy val values = AccessIssueOccurrenceValues(wrapped)
	
	lazy val joinIssueVariants = join(ScribeTables.issueVariant)
	lazy val whereIssueVariants = FilterByIssueVariant(joinIssueVariants)
	lazy val issueVariants = AccessIssueVariantValues(joinIssueVariants)
	
	lazy val joinIssues = joinIssueVariants.join(ScribeTables.issue)
	lazy val whereIssues = FilterByIssue(joinIssues)
	lazy val issues = AccessIssueValues(joinIssues)
}

/**
  * Provides access to row-specific issue occurrence -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueOccurrenceRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessIssueOccurrences[A, AccessIssueOccurrenceRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessIssueOccurrenceRows[A], AccessIssueOccurrence[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessIssueOccurrenceRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueOccurrence(target)
}

/**
  * Used for accessing issue occurrence items that have been combined with one-to-many 
  * combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessCombinedIssueOccurrences[A](wrapped: TargetingMany[A]) 
	extends AccessIssueOccurrences[A, AccessCombinedIssueOccurrences[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedIssueOccurrences[A], AccessIssueOccurrence[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedIssueOccurrences(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueOccurrence(target)
}

