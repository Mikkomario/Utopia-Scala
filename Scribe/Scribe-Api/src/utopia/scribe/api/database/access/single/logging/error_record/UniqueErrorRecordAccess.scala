package utopia.scribe.api.database.access.single.logging.error_record

import utopia.flow.collection.mutable.iterator.{OptionsIterator, PollableOnce}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.ErrorRecordFactory
import utopia.scribe.api.database.model.logging.ErrorRecordModel
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueErrorRecordAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueErrorRecordAccess = new _UniqueErrorRecordAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueErrorRecordAccess(condition: Condition) extends UniqueErrorRecordAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct error records.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueErrorRecordAccess 
	extends SingleRowModelAccess[ErrorRecord] with FilterableView[UniqueErrorRecordAccess] 
		with DistinctModelAccess[ErrorRecord, Option[ErrorRecord], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The name of this exception type. Typically the exception class name.. None if no error record (or value)
	  *  was found.
	  */
	def exceptionType(implicit connection: Connection) = pullColumn(model.exceptionTypeColumn).getString
	
	/**
	  * Id of the topmost stack trace element that corresponds to this error record. None if
	  *  no error record (or value) was found.
	  */
	def stackTraceId(implicit connection: Connection) = pullColumn(model.stackTraceIdColumn).int
	
	/**
	  * Id of the underlying error that caused this error/failure. None if this error represents the root problem..
	  *  None if no error record (or value) was found.
	  */
	def causeId(implicit connection: Connection) = pullColumn(model.causeIdColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ErrorRecordModel
	
	/**
	  * @param c Implicit DB connection - Should be kept open during the whole iteration
	  * @return An iterator that returns this error and all the underlying errors
	  */
	def topToBottomIterator(implicit c: Connection) =
		OptionsIterator.iterate(pull) { error =>
			error.causeId.flatMap { DbErrorRecord(_).pull }
		}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ErrorRecordFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueErrorRecordAccess = 
		new UniqueErrorRecordAccess._UniqueErrorRecordAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the cause ids of the targeted error records
	  * @param newCauseId A new cause id to assign
	  * @return Whether any error record was affected
	  */
	def causeId_=(newCauseId: Int)(implicit connection: Connection) = putColumn(model.causeIdColumn, 
		newCauseId)
	
	/**
	  * Updates the exception types of the targeted error records
	  * @param newExceptionType A new exception type to assign
	  * @return Whether any error record was affected
	  */
	def exceptionType_=(newExceptionType: String)(implicit connection: Connection) = 
		putColumn(model.exceptionTypeColumn, newExceptionType)
	
	/**
	  * Updates the stack trace ids of the targeted error records
	  * @param newStackTraceId A new stack trace id to assign
	  * @return Whether any error record was affected
	  */
	def stackTraceId_=(newStackTraceId: Int)(implicit connection: Connection) = 
		putColumn(model.stackTraceIdColumn, newStackTraceId)
}

