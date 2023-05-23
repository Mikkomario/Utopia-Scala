package utopia.scribe.api.database.access.single.logging.stack_trace_element

import utopia.scribe.api.database.factory.logging.StackTraceElementFactory
import utopia.scribe.api.database.model.logging.StackTraceElementModel
import utopia.scribe.core.model.stored.logging.StackTraceElement
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual stack trace elements
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbStackTraceElement extends SingleRowModelAccess[StackTraceElement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StackTraceElementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted stack trace element
	  * @return An access point to that stack trace element
	  */
	def apply(id: Int) = DbSingleStackTraceElement(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique stack trace elements.
	  * @return An access point to the stack trace element that satisfies the specified condition
	  */
	protected
		 def filterDistinct(condition: Condition) = UniqueStackTraceElementAccess(mergeCondition(condition))
}

