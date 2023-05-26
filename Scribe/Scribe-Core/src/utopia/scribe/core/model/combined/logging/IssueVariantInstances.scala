package utopia.scribe.core.model.combined.logging

import utopia.flow.operator.CombinedOrdering
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.{IssueOccurrence, IssueVariant}

object IssueVariantInstances
{
	/**
	  * Ordering that orders based on software version and last occurrence time
	  */
	implicit val ordering: Ordering[IssueVariantInstances] = CombinedOrdering(
		Ordering.by { v: IssueVariantInstances => v.version },
		Ordering.by { v: IssueVariantInstances => v.occurrences.iterator.map { _.lastOccurrence }.maxOption }
	)
}

/**
  * Adds specific occurrence/instance information to a single issue variant
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
case class IssueVariantInstances(variant: IssueVariant, occurrences: Vector[IssueOccurrence]) 
	extends Extender[IssueVariantData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this variant in the database
	  */
	def id = variant.id
	
	/**
	  * @return The earliest recorded occurrence of this issue variant.
	  *         None if no occurrences are recorded.
	  */
	def earliestOccurrence = occurrences.minByOption { _.firstOccurrence }
	/**
	  * @return The latest occurrence of this issue variant.
	  *         None if no occurrences are recorded.
	  */
	def latestOccurrence = occurrences.maxByOption { _.lastOccurrence }
	
	/**
	  * @return Total number of issue occurrences represented by this instance
	  */
	def numberOfOccurrences = occurrences.iterator.map { _.count }.sum
	/**
	  * @return The average time interval between the recorded occurrences
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
	
	override def wrapped = variant.data
}

