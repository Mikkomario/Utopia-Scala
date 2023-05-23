package utopia.scribe.api.database.access.single.logging.stack_trace_element

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.StackTraceElementFactory
import utopia.scribe.api.database.model.logging.StackTraceElementModel
import utopia.scribe.core.model.stored.logging.StackTraceElement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueStackTraceElementAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueStackTraceElementAccess =
		 new _UniqueStackTraceElementAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueStackTraceElementAccess(condition: Condition) extends UniqueStackTraceElementAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct stack trace elements.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueStackTraceElementAccess 
	extends SingleRowModelAccess[StackTraceElement] with FilterableView[UniqueStackTraceElementAccess] 
		with DistinctModelAccess[StackTraceElement, Option[StackTraceElement], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The class where this event was recorded.. None if no stack trace element (or value) was found.
	  */
	def className(implicit connection: Connection) = pullColumn(model.classNameColumn).getString
	
	/**
	  * The name of the class method where this event was recorded. None if no stack trace element (or value)
	  *  was found.
	  */
	def methodName(implicit connection: Connection) = pullColumn(model.methodNameColumn).getString
	
	/**
	  * 
		The code line number where this event was recorded. None if no stack trace element (or value) was found.
	  */
	def lineNumber(implicit connection: Connection) = pullColumn(model.lineNumberColumn).int
	
	/**
	  * 
		Id of the stack trace element that originated this element. I.e. the element directly before this element.
	  *  None if this is the root element.. None if no stack trace element (or value) was found.
	  */
	def causeId(implicit connection: Connection) = pullColumn(model.causeIdColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StackTraceElementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueStackTraceElementAccess = 
		new UniqueStackTraceElementAccess._UniqueStackTraceElementAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the cause ids of the targeted stack trace elements
	  * @param newCauseId A new cause id to assign
	  * @return Whether any stack trace element was affected
	  */
	def causeId_=(newCauseId: Int)(implicit connection: Connection) = putColumn(model.causeIdColumn, 
		newCauseId)
	
	/**
	  * Updates the class names of the targeted stack trace elements
	  * @param newClassName A new class name to assign
	  * @return Whether any stack trace element was affected
	  */
	def className_=(newClassName: String)(implicit connection: Connection) = 
		putColumn(model.classNameColumn, newClassName)
	
	/**
	  * Updates the line numbers of the targeted stack trace elements
	  * @param newLineNumber A new line number to assign
	  * @return Whether any stack trace element was affected
	  */
	def lineNumber_=(newLineNumber: Int)(implicit connection: Connection) = 
		putColumn(model.lineNumberColumn, newLineNumber)
	
	/**
	  * Updates the method names of the targeted stack trace elements
	  * @param newMethodName A new method name to assign
	  * @return Whether any stack trace element was affected
	  */
	def methodName_=(newMethodName: String)(implicit connection: Connection) = 
		putColumn(model.methodNameColumn, newMethodName)
}

