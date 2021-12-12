package utopia.keep.database.access.single.logging

import utopia.keep.database.factory.logging.ProblemFactory
import utopia.keep.database.model.logging.ProblemModel
import utopia.keep.model.stored.logging.Problem
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Problems
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbProblem extends SingleRowModelAccess[Problem] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Problem instance
	  * @return An access point to that Problem
	  */
	def apply(id: Int) = DbSingleProblem(id)
}

