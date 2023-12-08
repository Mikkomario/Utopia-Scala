package utopia.scribe.api.database.access.many.logging.error_record

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple error records at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbErrorRecords extends ManyErrorRecordsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted error records
	  * @return An access point to error records with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbErrorRecordsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbErrorRecordsSubset(targetIds: Set[Int]) extends ManyErrorRecordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

