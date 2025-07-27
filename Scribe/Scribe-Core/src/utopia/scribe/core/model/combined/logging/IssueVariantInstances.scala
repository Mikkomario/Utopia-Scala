package utopia.scribe.core.model.combined.logging

import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.time.TimeExtensions._
import utopia.scribe.core.model.stored.logging.{IssueOccurrence, IssueVariant}

object IssueVariantInstances
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Ordering that orders based on software version and last occurrence time
	  */
	implicit val ordering: Ordering[IssueVariantInstances] = CombinedOrdering(
		Ordering.by { v: IssueVariantInstances => v.version },
		Ordering.by { v: IssueVariantInstances => v.occurrences.iterator.map { _.lastOccurrence }.maxOption })
	
	
	// OTHER	--------------------
	
	/**
	  * @param variant     variant to wrap
	  * @param occurrences occurrences to attach to this variant
	  * @return Combination of the specified variant and occurrence
	  */
	def apply(variant: IssueVariant, occurrences: Seq[IssueOccurrence]): IssueVariantInstances = 
		_IssueVariantInstances(variant, occurrences)
	
	
	// NESTED	--------------------
	
	/**
	  * @param variant     variant to wrap
	  * @param occurrences occurrences to attach to this variant
	  */
	private case class _IssueVariantInstances(variant: IssueVariant, occurrences: Seq[IssueOccurrence]) 
		extends IssueVariantInstances
	{
		// IMPLEMENTED	--------------------
		
		override protected def wrap(factory: IssueVariant) = copy(variant = factory)
	}
}

/**
  * Adds specific occurrence/instance information to a single issue variant
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueVariantInstances extends CombinedIssueVariant[IssueVariantInstances]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped issue variant
	  */
	def variant: IssueVariant
	/**
	  * Occurrences that are attached to this variant
	  */
	def occurrences: Seq[IssueOccurrence]
	
	
	// COMPUTED ------------------------
	
	/**
	  * The earliest recorded occurrence of this issue variant.
	  * None if no occurrences are recorded.
	  */
	def earliestOccurrence = occurrences.minByOption { _.firstOccurrence }
	/**
	  * The latest occurrence of this issue variant.
	  * None if no occurrences are recorded.
	  */
	def latestOccurrence = occurrences.maxByOption { _.lastOccurrence }
	/**
	  * Total number of issue occurrences represented by this instance
	  */
	def numberOfOccurrences = occurrences.iterator.map { _.count }.sum
	
	/**
	  * The average time interval between the recorded occurrences
	  */
	def averageOccurrenceInterval = {
		val count = numberOfOccurrences
		if (count > 1)
			earliestOccurrence.flatMap { earliest =>
				latestOccurrence.map { latest =>
					(latest.lastOccurrence - earliest.firstOccurrence) / count
				}
			}
		else
			None
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def issueVariant = variant
}
