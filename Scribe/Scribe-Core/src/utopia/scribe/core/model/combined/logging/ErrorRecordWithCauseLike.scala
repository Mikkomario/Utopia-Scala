package utopia.scribe.core.model.combined.logging

import utopia.scribe.core.model.factory.logging.ErrorRecordFactoryWrapper
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.ErrorRecord

/**
 * Common trait for combined error record models that include the cause
 * @author Mikko Hilpinen
 * @since 08.05.2026, v1.2.2
 */
trait ErrorRecordWithCauseLike[+Repr <: ErrorRecord]
	extends ErrorRecord with ErrorRecordFactoryWrapper[ErrorRecordData, Repr]
{
	/**
	 * @return Record of the cause of this error
	 */
	def cause: Option[Repr]
}
