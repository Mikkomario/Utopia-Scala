package utopia.scribe.core.model.stored.logging

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.scribe.core.model.factory.logging.IssueOccurrenceFactoryWrapper
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.vault.store.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object IssueOccurrence extends StoredFromModelFactory[IssueOccurrenceData, IssueOccurrence]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Ordering that orders based on last occurrence time
	  */
	implicit val chronoOrdering: Ordering[IssueOccurrence] = Ordering.by { _.data }
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueOccurrenceData
	
	override protected def complete(model: HasProperties, data: IssueOccurrenceData) =
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a issue occurrence that has already been stored in the database
  * @param id   id of this issue occurrence in the database
  * @param data Wrapped issue occurrence data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueOccurrence(id: Int, data: IssueOccurrenceData) 
	extends StoredModelConvertible[IssueOccurrenceData] with FromIdFactory[Int, IssueOccurrence] 
		with IssueOccurrenceFactoryWrapper[IssueOccurrenceData, IssueOccurrence]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: IssueOccurrenceData) = copy(data = data)
}

