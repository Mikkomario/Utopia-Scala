package utopia.ambassador.database.access.many.service

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple AuthServices at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthServices extends ManyAuthServicesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthServices
	  * @return An access point to AuthServices with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthServicesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthServicesSubset(targetIds: Set[Int]) extends ManyAuthServicesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

