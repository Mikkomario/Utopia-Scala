package utopia.scribe.core.model.combined.logging

import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.ErrorRecord

object ErrorRecordWithCause
{
	// OTHER    ------------------------
	
	/**
	 * @param id ID of this record
	 * @param data Data of this record
	 * @param cause Record of the causing error, if applicable
	 * @return A new error record
	 */
	def apply(id: Int, data: ErrorRecordData, cause: Option[ErrorRecordWithCause] = None): ErrorRecordWithCause =
		_ErrorRecordWithCause(id, data, cause)
	
	
	// NESTED   ------------------------
	
	private case class _ErrorRecordWithCause(id: Int, data: ErrorRecordData, cause: Option[ErrorRecordWithCause])
		extends ErrorRecordWithCause
	{
		override protected def wrap(factory: ErrorRecordData): ErrorRecordWithCause = copy(data = factory)
		override def withId(id: Int): ErrorRecord = copy(id = id)
	}
}

/**
 * Includes a record of the causing error, in an error record
 * @author Mikko Hilpinen
 * @since 08.05.2026, v0.4
 */
trait ErrorRecordWithCause extends ErrorRecordWithCauseLike[ErrorRecordWithCause]
