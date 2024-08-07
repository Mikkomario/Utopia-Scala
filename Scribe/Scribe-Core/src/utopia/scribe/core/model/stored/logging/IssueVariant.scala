package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.IssueVariantData

object IssueVariant extends StoredFromModelFactory[IssueVariant, IssueVariantData]
{
	// ATTRIBUTES   --------------------
	
	/**
	  * Ordering that orders by variant version and creation time
	  */
	implicit val ordering: Ordering[IssueVariant] = Ordering.by { _.data }
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueVariantData
}

/**
  * Represents a issue variant that has already been stored in the database
  * @param id id of this issue variant in the database
  * @param data Wrapped issue variant data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueVariant(id: Int, data: IssueVariantData) extends StoredModelConvertible[IssueVariantData]
{
	/**
	  * @param occurrences Occurrences to attach to this issue variant
	  * @return Model that contains this issue variant with the specified occurrences
	  */
	def withOccurrences(occurrences: Seq[IssueOccurrence]) = IssueVariantInstances(this, occurrences)
}
