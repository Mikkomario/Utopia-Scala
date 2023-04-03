package utopia.scribe.database.access.many.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple ProblemCases at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbProblemCases extends ManyProblemCasesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted ProblemCases
	  * @return An access point to ProblemCases with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbProblemCasesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbProblemCasesSubset(targetIds: Set[Int]) extends ManyProblemCasesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

