package utopia.scribe.database.access.single.logging

import utopia.scribe.database.factory.logging.ProblemCaseFactory
import utopia.scribe.database.model.logging.ProblemCaseModel
import utopia.scribe.model.stored.logging.ProblemCase
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual ProblemCases
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbProblemCase extends SingleRowModelAccess[ProblemCase] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemCaseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemCaseFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted ProblemCase instance
	  * @return An access point to that ProblemCase
	  */
	def apply(id: Int) = DbSingleProblemCase(id)
}

