package utopia.scribe.api.database.access.logging.issue.variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.api.database.access.logging.issue.occurrence.{AccessIssueOccurrenceValues, FilterByIssueOccurrence}
import utopia.scribe.api.database.access.logging.issue.{AccessIssueValues, FilterByIssue}
import utopia.scribe.api.database.reader.logging.{ContextualIssueVariantDbReader, IssueVariantDbReader, IssueVariantInstancesDbReader}
import utopia.scribe.api.database.storable.logging.{IssueOccurrenceDbModel, IssueVariantDbModel}
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many._
import utopia.vault.nosql.targeting.one.TargetingOne

import java.time.Instant
import scala.language.implicitConversions

object AccessIssueVariants 
	extends AccessManyRoot[AccessIssueVariantRows[IssueVariant]] with WrapRowAccess[AccessIssueVariantRows] 
		with WrapOneToManyAccess[AccessCombinedIssueVariants]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(IssueVariantDbReader)
	
	/**
	  * Access to issue variants in the DB, also including issue information
	  */
	lazy val withIssues = AccessIssueVariantRows(AccessManyRows(ContextualIssueVariantDbReader))
	/**
	  * Access to issue variants in the DB, also including issue occurrence information
	  */
	lazy val instances = AccessCombinedIssueVariants(AccessMany(IssueVariantInstancesDbReader))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessIssueVariants[_, _]): AccessIssueVariantValues = access.values
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingManyRows[A]) = AccessIssueVariantRows(access)
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingMany[A]) = AccessCombinedIssueVariants(access)
}

/**
  * Used for accessing multiple issue variants from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
abstract class AccessIssueVariants[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessIssueVariant[A]] with FilterIssueVariants[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible issue variants
	  */
	lazy val values = AccessIssueVariantValues(wrapped)
	
	/**
	  * A copy of this access which also targets issue
	  */
	lazy val joinIssues = join(ScribeTables.issue)
	/**
	  * Access to the values of linked issues
	  */
	lazy val issues = AccessIssueValues(joinIssues)
	/**
	  * Access to issue -based filtering functions
	  */
	lazy val whereIssues = FilterByIssue(joinIssues)
	
	/**
	  * A copy of this access which also targets issue_occurrence
	  */
	lazy val joinOccurrences = join(occurrenceModel.table)
	/**
	  * Access to the values of linked issue occurrences
	  */
	lazy val occurrences = AccessIssueOccurrenceValues(joinOccurrences)
	/**
	  * Access to issue occurrence -based filtering functions
	  */
	lazy val whereOccurrences = FilterByIssueOccurrence(joinOccurrences)
	
	
	// COMPUTED ------------------------
	
	protected def occurrenceModel = IssueOccurrenceDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def timestampColumn = IssueVariantDbModel.created
	
	
	// OTHER    ------------------------
	
	/**
	  * @param threshold A time threshold (inclusive)
	  * @return Access to issue variants which have not occurred since the specified time threshold
	  */
	def notOccurredSince(threshold: Instant) =
		leftJoin(occurrenceModel.table.where(occurrenceModel.latest >= threshold)).filter(occurrenceModel.id.isNull)
}

/**
  * Provides access to row-specific issue variant -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueVariantRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessIssueVariants[A, AccessIssueVariantRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessIssueVariantRows[A], AccessIssueVariant[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessIssueVariantRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueVariant(target)
}

/**
  * Used for accessing issue variant items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessCombinedIssueVariants[A](wrapped: TargetingMany[A]) 
	extends AccessIssueVariants[A, AccessCombinedIssueVariants[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedIssueVariants[A], AccessIssueVariant[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedIssueVariants(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueVariant(target)
}

