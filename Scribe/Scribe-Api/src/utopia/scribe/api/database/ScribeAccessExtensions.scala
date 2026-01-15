package utopia.scribe.api.database

import utopia.scribe.api.database.access.logging.error.stack.AccessStackTraceElementRecord
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
		def stackAccess = AccessStackTraceElementRecord(e.stackTraceId)
	}
}
