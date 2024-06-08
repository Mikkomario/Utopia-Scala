package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.combined.logging.{IssueInstances, IssueVariantInstances}
import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.IssueData

object Issue extends StoredFromModelFactory[Issue, IssueData]
{
	// ATTRIBUTES   --------------------
	
	/**
	  * Ordering that presents the least severe issues first
	  */
	implicit val ordering: Ordering[Issue] = Ordering.by { _.data }
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueData
}

/**
  * Represents a issue that has already been stored in the database
  * @param id id of this issue in the database
  * @param data Wrapped issue data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class Issue(id: Int, data: IssueData) extends StoredModelConvertible[IssueData]
{
	/**
	  * @param instances Occurrences of this issue, grouped by issue variant
	  * @return Copy of this issue which includes the specified issue instances
	  */
	def withOccurrences(instances: Seq[IssueVariantInstances]) = IssueInstances(this, instances)
}
