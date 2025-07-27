package utopia.scribe.core.model.stored.logging

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.scribe.core.model.factory.logging.IssueVariantFactoryWrapper
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.vault.store.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object IssueVariant extends StoredFromModelFactory[IssueVariantData, IssueVariant]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Ordering that orders by variant version and creation time
	  */
	implicit val ordering: Ordering[IssueVariant] = Ordering.by { _.data }
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueVariantData
	
	override protected def complete(model: AnyModel, data: IssueVariantData) = 
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a issue variant that has already been stored in the database
  * @param id   id of this issue variant in the database
  * @param data Wrapped issue variant data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueVariant(id: Int, data: IssueVariantData) 
	extends StoredModelConvertible[IssueVariantData] with FromIdFactory[Int, IssueVariant] 
		with IssueVariantFactoryWrapper[IssueVariantData, IssueVariant]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: IssueVariantData) = copy(data = data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param occurrences Occurrences to attach to this issue variant
	  * @return Model that contains this issue variant with the specified occurrences
	  */
	def withOccurrences(occurrences: Seq[IssueOccurrence]) = IssueVariantInstances(this, occurrences)
}

