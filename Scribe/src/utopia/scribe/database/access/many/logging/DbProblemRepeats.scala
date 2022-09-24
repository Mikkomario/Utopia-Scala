package utopia.scribe.database.access.many.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple ProblemRepeats at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbProblemRepeats extends ManyProblemRepeatsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted ProblemRepeats
	  * @return An access point to ProblemRepeats with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbProblemRepeatsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbProblemRepeatsSubset(targetIds: Set[Int]) extends ManyProblemRepeatsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

