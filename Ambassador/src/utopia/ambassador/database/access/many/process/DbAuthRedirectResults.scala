package utopia.ambassador.database.access.many.process

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple AuthRedirectResults at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthRedirectResults extends ManyAuthRedirectResultsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthRedirectResults
	  * @return An access point to AuthRedirectResults with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthRedirectResultsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthRedirectResultsSubset(targetIds: Set[Int]) extends ManyAuthRedirectResultsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

