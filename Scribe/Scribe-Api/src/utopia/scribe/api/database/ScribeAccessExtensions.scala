package utopia.scribe.api.database

import utopia.scribe.api.database.access.single.logging.stack_trace_element_record.DbStackTraceElementRecord
import utopia.scribe.core.model.stored.logging.ErrorRecord

/**
  * Extensions enabling easier access to the DB
  * @author Mikko Hilpinen
  * @since 26.5.2023, v0.1
  */
object ScribeAccessExtensions
{
	implicit class AccessibleError(val e: ErrorRecord) extends AnyVal
	{
		/**
		  * @return Access to the stack trace of this error
		  */
		def stackAccess = DbStackTraceElementRecord(e.stackTraceId)
	}
}
