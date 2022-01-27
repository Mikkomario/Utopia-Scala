package utopia.scribe.database.access.single.logging

import utopia.scribe.database.factory.logging.ProblemRepeatFactory
import utopia.scribe.database.model.logging.ProblemRepeatModel
import utopia.scribe.model.stored.logging.ProblemRepeat
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual ProblemRepeats
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbProblemRepeat extends SingleRowModelAccess[ProblemRepeat] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemRepeatModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemRepeatFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted ProblemRepeat instance
	  * @return An access point to that ProblemRepeat
	  */
	def apply(id: Int) = DbSingleProblemRepeat(id)
}

