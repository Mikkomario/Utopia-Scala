package utopia.scribe.database.access.many.logging

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple Problems at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbProblems extends ManyProblemsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Problems
	  * @return An access point to Problems with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbProblemsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbProblemsSubset(targetIds: Set[Int]) extends ManyProblemsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

