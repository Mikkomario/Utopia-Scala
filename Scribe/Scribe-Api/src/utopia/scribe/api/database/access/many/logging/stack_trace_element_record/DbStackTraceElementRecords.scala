package utopia.scribe.api.database.access.many.logging.stack_trace_element_record

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple stack trace element records at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbStackTraceElementRecords extends ManyStackTraceElementRecordsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted stack trace element records
	  * @return An access point to stack trace element records with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbStackTraceElementRecordsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbStackTraceElementRecordsSubset(targetIds: Set[Int]) extends ManyStackTraceElementRecordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

