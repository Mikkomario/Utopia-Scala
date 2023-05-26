package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData

object IssueOccurrence extends StoredFromModelFactory[IssueOccurrence, IssueOccurrenceData]
{
	// ATTRIBUTES   --------------------
	
	/**
	  * Ordering that orders based on last occurrence time
	  */
	implicit val chronoOrdering: Ordering[IssueOccurrence] = Ordering.by { _.data }
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueOccurrenceData
}

/**
  * Represents a issue occurrence that has already been stored in the database
  * @param id id of this issue occurrence in the database
  * @param data Wrapped issue occurrence data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueOccurrence(id: Int, data: IssueOccurrenceData) 
	extends StoredModelConvertible[IssueOccurrenceData]

