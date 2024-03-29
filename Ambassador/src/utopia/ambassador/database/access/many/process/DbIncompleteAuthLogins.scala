package utopia.ambassador.database.access.many.process

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple IncompleteAuthLogins at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbIncompleteAuthLogins extends ManyIncompleteAuthLoginsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted IncompleteAuthLogins
	  * @return An access point to IncompleteAuthLogins with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbIncompleteAuthLoginsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbIncompleteAuthLoginsSubset(targetIds: Set[Int]) extends ManyIncompleteAuthLoginsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

