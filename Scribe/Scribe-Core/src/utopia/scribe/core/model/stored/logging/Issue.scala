package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.IssueData

object Issue extends StoredFromModelFactory[Issue, IssueData]
{
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

