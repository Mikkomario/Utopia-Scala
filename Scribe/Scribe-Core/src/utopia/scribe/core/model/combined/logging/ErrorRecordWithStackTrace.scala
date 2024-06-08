package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.{ErrorRecord, StackTraceElementRecord}

/**
  * Combines error record data with stack trace information
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
case class ErrorRecordWithStackTrace(error: ErrorRecord,
                                     stackTraceElements: Seq[StackTraceElementRecord] = Empty,
                                     cause: Option[ErrorRecordWithStackTrace] = None)
	extends Extender[ErrorRecordData]
{
	// COMPUTED --------------------------
	
	/**
	  * @return Id of this error
	  */
	def id = error.id
	
	
	// IMPLEMENTED  ----------------------
	
	override def wrapped: ErrorRecordData = error.data
}
