package utopia.scribe.api.database.access.many.logging.error_record

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.factory.logging.ErrorRecordFactory
import utopia.scribe.api.database.model.logging.ErrorRecordModel
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object ManyErrorRecordsAccess
{
	// NESTED	--------------------
	
	private class ManyErrorRecordsSubView(condition: Condition) extends ManyErrorRecordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple error records at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyErrorRecordsAccess 
	extends ManyRowModelAccess[ErrorRecord] with FilterableView[ManyErrorRecordsAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * exception types of the accessible error records
	  */
	def exceptionTypes(implicit connection: Connection) = 
		pullColumn(model.exceptionTypeColumn).flatMap { _.string }
	
	/**
	  * stack trace ids of the accessible error records
	  */
	def stackTraceIds(implicit connection: Connection) = 
		pullColumn(model.stackTraceIdColumn).map { v => v.getInt }
	
	/**
	  * cause ids of the accessible error records
	  */
	def causeIds(implicit connection: Connection) = pullColumn(model.causeIdColumn).flatMap { v => v.int }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ErrorRecordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ErrorRecordFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyErrorRecordsAccess = 
		new ManyErrorRecordsAccess.ManyErrorRecordsSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the cause ids of the targeted error records
	  * @param newCauseId A new cause id to assign
	  * @return Whether any error record was affected
	  */
	def causeIds_=(newCauseId: Int)(implicit connection: Connection) = putColumn(model.causeIdColumn, 
		newCauseId)
	
	/**
	  * Updates the exception types of the targeted error records
	  * @param newExceptionType A new exception type to assign
	  * @return Whether any error record was affected
	  */
	def exceptionTypes_=(newExceptionType: String)(implicit connection: Connection) = 
		putColumn(model.exceptionTypeColumn, newExceptionType)
	
	/**
	  * Updates the stack trace ids of the targeted error records
	  * @param newStackTraceId A new stack trace id to assign
	  * @return Whether any error record was affected
	  */
	def stackTraceIds_=(newStackTraceId: Int)(implicit connection: Connection) = 
		putColumn(model.stackTraceIdColumn, newStackTraceId)
}

