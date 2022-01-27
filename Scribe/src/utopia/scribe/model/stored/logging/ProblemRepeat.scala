package utopia.scribe.model.stored.logging

import utopia.scribe.database.access.single.logging.DbSingleProblemRepeat
import utopia.scribe.model.partial.logging.ProblemRepeatData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a ProblemRepeat that has already been stored in the database
  * @param id id of this ProblemRepeat in the database
  * @param data Wrapped ProblemRepeat data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemRepeat(id: Int, data: ProblemRepeatData) extends StoredModelConvertible[ProblemRepeatData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this ProblemRepeat in the database
	  */
	def access = DbSingleProblemRepeat(id)
}

