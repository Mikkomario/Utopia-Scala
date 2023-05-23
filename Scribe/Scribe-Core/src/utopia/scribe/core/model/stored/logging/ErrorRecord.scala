package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.ErrorRecordData

object ErrorRecord extends StoredFromModelFactory[ErrorRecord, ErrorRecordData]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = ErrorRecordData
}

/**
  * Represents a error record that has already been stored in the database
  * @param id id of this error record in the database
  * @param data Wrapped error record data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class ErrorRecord(id: Int, data: ErrorRecordData) extends StoredModelConvertible[ErrorRecordData]

