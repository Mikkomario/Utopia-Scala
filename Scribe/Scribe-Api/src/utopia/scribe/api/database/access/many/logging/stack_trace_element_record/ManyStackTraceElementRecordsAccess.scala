package utopia.scribe.api.database.access.many.logging.stack_trace_element_record

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.factory.logging.StackTraceElementRecordFactory
import utopia.scribe.api.database.model.logging.StackTraceElementRecordModel
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object ManyStackTraceElementRecordsAccess
{
	// NESTED	--------------------
	
	private class ManyStackTraceElementRecordsSubView(condition: Condition) 
		extends ManyStackTraceElementRecordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple stack trace element records at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyStackTraceElementRecordsAccess 
	extends ManyRowModelAccess[StackTraceElementRecord] 
		with FilterableView[ManyStackTraceElementRecordsAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * file names of the accessible stack trace element records
	  */
	def fileNames(implicit connection: Connection) = pullColumn(model.fileNameColumn).flatMap { _.string }
	
	/**
	  * class names of the accessible stack trace element records
	  */
	def classNames(implicit connection: Connection) = pullColumn(model.classNameColumn).flatMap { _.string }
	
	/**
	  * method names of the accessible stack trace element records
	  */
	def methodNames(implicit connection: Connection) = pullColumn(model.methodNameColumn).flatMap { _.string }
	
	/**
	  * line numbers of the accessible stack trace element records
	  */
	def lineNumbers(implicit connection: Connection) = pullColumn(model.lineNumberColumn)
		.flatMap { v => v.int }
	
	/**
	  * cause ids of the accessible stack trace element records
	  */
	def causeIds(implicit connection: Connection) = pullColumn(model.causeIdColumn).flatMap { v => v.int }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StackTraceElementRecordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementRecordFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyStackTraceElementRecordsAccess = 
		new ManyStackTraceElementRecordsAccess
			.ManyStackTraceElementRecordsSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the cause ids of the targeted stack trace element records
	  * @param newCauseId A new cause id to assign
	  * @return Whether any stack trace element record was affected
	  */
	def causeIds_=(newCauseId: Int)(implicit connection: Connection) = putColumn(model.causeIdColumn, 
		newCauseId)
	
	/**
	  * Updates the class names of the targeted stack trace element records
	  * @param newClassName A new class name to assign
	  * @return Whether any stack trace element record was affected
	  */
	def classNames_=(newClassName: String)(implicit connection: Connection) = 
		putColumn(model.classNameColumn, newClassName)
	
	/**
	  * Updates the file names of the targeted stack trace element records
	  * @param newFileName A new file name to assign
	  * @return Whether any stack trace element record was affected
	  */
	def fileNames_=(newFileName: String)(implicit connection: Connection) = 
		putColumn(model.fileNameColumn, newFileName)
	
	/**
	  * Updates the line numbers of the targeted stack trace element records
	  * @param newLineNumber A new line number to assign
	  * @return Whether any stack trace element record was affected
	  */
	def lineNumbers_=(newLineNumber: Int)(implicit connection: Connection) = 
		putColumn(model.lineNumberColumn, newLineNumber)
	
	/**
	  * Updates the method names of the targeted stack trace element records
	  * @param newMethodName A new method name to assign
	  * @return Whether any stack trace element record was affected
	  */
	def methodNames_=(newMethodName: String)(implicit connection: Connection) = 
		putColumn(model.methodNameColumn, newMethodName)
}

