package utopia.scribe.api.database.model.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.ErrorRecordFactory
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ErrorRecordModel instances and for inserting error records to the database
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object ErrorRecordModel extends DataInserter[ErrorRecordModel, ErrorRecord, ErrorRecordData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains error record exception type
	  */
	val exceptionTypeAttName = "exceptionType"
	
	/**
	  * Name of the property that contains error record stack trace id
	  */
	val stackTraceIdAttName = "stackTraceId"
	
	/**
	  * Name of the property that contains error record cause id
	  */
	val causeIdAttName = "causeId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains error record exception type
	  */
	def exceptionTypeColumn = table(exceptionTypeAttName)
	
	/**
	  * Column that contains error record stack trace id
	  */
	def stackTraceIdColumn = table(stackTraceIdAttName)
	
	/**
	  * Column that contains error record cause id
	  */
	def causeIdColumn = table(causeIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ErrorRecordFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ErrorRecordData) = 
		apply(None, data.exceptionType, Some(data.stackTraceId), data.causeId)
	
	override protected def complete(id: Value, data: ErrorRecordData) = ErrorRecord(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * 
		@param causeId Id of the underlying error that caused this error/failure. None if this error represents the
	  *  root problem.
	  * @return A model containing only the specified cause id
	  */
	def withCauseId(causeId: Int) = apply(causeId = Some(causeId))
	
	/**
	  * @param exceptionType The name of this exception type. Typically the exception class name.
	  * @return A model containing only the specified exception type
	  */
	def withExceptionType(exceptionType: String) = apply(exceptionType = exceptionType)
	
	/**
	  * @param id A error record id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param stackTraceId Id of the topmost stack trace element that corresponds to this error record
	  * @return A model containing only the specified stack trace id
	  */
	def withStackTraceId(stackTraceId: Int) = apply(stackTraceId = Some(stackTraceId))
}

/**
  * Used for interacting with ErrorRecords in the database
  * @param id error record database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class ErrorRecordModel(id: Option[Int] = None, exceptionType: String = "", 
	stackTraceId: Option[Int] = None, causeId: Option[Int] = None) 
	extends StorableWithFactory[ErrorRecord]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ErrorRecordModel.factory
	
	override def valueProperties = {
		import ErrorRecordModel._
		Vector("id" -> id, exceptionTypeAttName -> exceptionType, stackTraceIdAttName -> stackTraceId, 
			causeIdAttName -> causeId)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * 
		@param causeId Id of the underlying error that caused this error/failure. None if this error represents the
	  *  root problem.
	  * @return A new copy of this model with the specified cause id
	  */
	def withCauseId(causeId: Int) = copy(causeId = Some(causeId))
	
	/**
	  * @param exceptionType The name of this exception type. Typically the exception class name.
	  * @return A new copy of this model with the specified exception type
	  */
	def withExceptionType(exceptionType: String) = copy(exceptionType = exceptionType)
	
	/**
	  * @param stackTraceId Id of the topmost stack trace element that corresponds to this error record
	  * @return A new copy of this model with the specified stack trace id
	  */
	def withStackTraceId(stackTraceId: Int) = copy(stackTraceId = Some(stackTraceId))
}

