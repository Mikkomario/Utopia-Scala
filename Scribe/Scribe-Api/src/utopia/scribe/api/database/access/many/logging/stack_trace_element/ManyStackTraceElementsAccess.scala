package utopia.scribe.api.database.access.many.logging.stack_trace_element

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.factory.logging.StackTraceElementFactory
import utopia.scribe.api.database.model.logging.StackTraceElementModel
import utopia.scribe.core.model.stored.logging.StackTraceElement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object ManyStackTraceElementsAccess
{
	// NESTED	--------------------
	
	private class ManyStackTraceElementsSubView(condition: Condition) extends ManyStackTraceElementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple stack trace elements at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyStackTraceElementsAccess 
	extends ManyRowModelAccess[StackTraceElement] with FilterableView[ManyStackTraceElementsAccess] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * class names of the accessible stack trace elements
	  */
	def classNames(implicit connection: Connection) = pullColumn(model.classNameColumn).flatMap { _.string }
	
	/**
	  * method names of the accessible stack trace elements
	  */
	def methodNames(implicit connection: Connection) = pullColumn(model.methodNameColumn).flatMap { _.string }
	
	/**
	  * line numbers of the accessible stack trace elements
	  */
	def lineNumbers(implicit connection: Connection) = pullColumn(model.lineNumberColumn)
		.map { v => v.getInt }
	
	/**
	  * cause ids of the accessible stack trace elements
	  */
	def causeIds(implicit connection: Connection) = pullColumn(model.causeIdColumn).flatMap { v => v.int }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StackTraceElementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyStackTraceElementsAccess = 
		new ManyStackTraceElementsAccess.ManyStackTraceElementsSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the cause ids of the targeted stack trace elements
	  * @param newCauseId A new cause id to assign
	  * @return Whether any stack trace element was affected
	  */
	def causeIds_=(newCauseId: Int)(implicit connection: Connection) = putColumn(model.causeIdColumn, 
		newCauseId)
	
	/**
	  * Updates the class names of the targeted stack trace elements
	  * @param newClassName A new class name to assign
	  * @return Whether any stack trace element was affected
	  */
	def classNames_=(newClassName: String)(implicit connection: Connection) = 
		putColumn(model.classNameColumn, newClassName)
	
	/**
	  * Updates the line numbers of the targeted stack trace elements
	  * @param newLineNumber A new line number to assign
	  * @return Whether any stack trace element was affected
	  */
	def lineNumbers_=(newLineNumber: Int)(implicit connection: Connection) = 
		putColumn(model.lineNumberColumn, newLineNumber)
	
	/**
	  * Updates the method names of the targeted stack trace elements
	  * @param newMethodName A new method name to assign
	  * @return Whether any stack trace element was affected
	  */
	def methodNames_=(newMethodName: String)(implicit connection: Connection) = 
		putColumn(model.methodNameColumn, newMethodName)
}

