package utopia.ambassador.database.access.many.process

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple AuthCompletionRedirectTargets at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthCompletionRedirectTargets extends ManyAuthCompletionRedirectTargetsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthCompletionRedirectTargets
	  * @return An access point to AuthCompletionRedirectTargets with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthCompletionRedirectTargetsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthCompletionRedirectTargetsSubset(targetIds: Set[Int]) 
		extends ManyAuthCompletionRedirectTargetsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

