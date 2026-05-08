package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.{ErrorRecord, StackTraceElementRecord}

/**
  * Combines error record data with stack trace information
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
case class ErrorRecordWithStackTrace(id: Int, data: ErrorRecordData,
                                     stackTraceElements: Seq[StackTraceElementRecord] = Empty,
                                     cause: Option[ErrorRecordWithStackTrace] = None)
	extends ErrorRecordWithCause with ErrorRecordWithCauseLike[ErrorRecordWithStackTrace]
{
	// COMPUTED --------------------------
	
	@deprecated("Deprecated for removal. This class already extends ErrorRecord.", "v1.2.2")
	def error = this
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def wrap(factory: ErrorRecordData): ErrorRecordWithStackTrace = copy(data = factory)
	override def withId(id: Int): ErrorRecord = copy(id = id)
}
