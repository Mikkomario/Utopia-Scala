package utopia.ambassador.database.access.many.process

import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple AuthPreparations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthPreparations extends ManyAuthPreparationsAccess with NonDeprecatedView[AuthPreparation]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthPreparations
	  * @return An access point to AuthPreparations with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthPreparationsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthPreparationsSubset(targetIds: Set[Int]) extends ManyAuthPreparationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

