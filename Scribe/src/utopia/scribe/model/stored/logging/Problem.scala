package utopia.scribe.model.stored.logging

import utopia.scribe.database.access.single.logging.DbSingleProblem
import utopia.scribe.model.partial.logging.ProblemData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a Problem that has already been stored in the database
  * @param id id of this Problem in the database
  * @param data Wrapped Problem data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class Problem(id: Int, data: ProblemData) extends StoredModelConvertible[ProblemData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this Problem in the database
	  */
	def access = DbSingleProblem(id)
}

