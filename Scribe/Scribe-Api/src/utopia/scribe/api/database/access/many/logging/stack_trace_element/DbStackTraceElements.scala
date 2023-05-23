package utopia.scribe.api.database.access.many.logging.stack_trace_element

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple stack trace elements at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbStackTraceElements extends ManyStackTraceElementsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted stack trace elements
	  * @return An access point to stack trace elements with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbStackTraceElementsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbStackTraceElementsSubset(targetIds: Set[Int]) extends ManyStackTraceElementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

