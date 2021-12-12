package utopia.keep.model.stored.logging

import utopia.keep.database.access.single.logging.DbSingleProblemCase
import utopia.keep.model.partial.logging.ProblemCaseData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a ProblemCase that has already been stored in the database
  * @param id id of this ProblemCase in the database
  * @param data Wrapped ProblemCase data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemCase(id: Int, data: ProblemCaseData) extends StoredModelConvertible[ProblemCaseData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this ProblemCase in the database
	  */
	def access = DbSingleProblemCase(id)
}

