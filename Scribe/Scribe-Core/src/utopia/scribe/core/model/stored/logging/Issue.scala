package utopia.scribe.core.model.stored.logging

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.scribe.core.model.combined.logging.{IssueInstances, IssueVariantInstances}
import utopia.scribe.core.model.factory.logging.IssueFactoryWrapper
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.vault.store.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object Issue extends StoredFromModelFactory[IssueData, Issue]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Ordering that presents the least severe issues first
	  */
	implicit val ordering: Ordering[Issue] = Ordering.by { _.data }
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueData
	
	override protected def complete(model: AnyModel, data: IssueData) = model("id").tryInt.map { apply(_, 
		data) }
}

/**
  * Represents a issue that has already been stored in the database
  * @param id   id of this issue in the database
  * @param data Wrapped issue data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class Issue(id: Int, data: IssueData) 
	extends StoredModelConvertible[IssueData] with FromIdFactory[Int, Issue] 
		with IssueFactoryWrapper[IssueData, Issue]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: IssueData) = copy(data = data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param instances Occurrences of this issue, grouped by issue variant
	  * @return Copy of this issue which includes the specified issue instances
	  */
	def withOccurrences(instances: Seq[IssueVariantInstances]) = IssueInstances(this, instances)
}

