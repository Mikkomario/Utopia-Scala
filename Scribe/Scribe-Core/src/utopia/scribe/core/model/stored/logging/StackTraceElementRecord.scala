package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData

object StackTraceElementRecord extends StoredFromModelFactory[StackTraceElementRecord, StackTraceElementRecordData]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = StackTraceElementRecordData
}

/**
  * Represents a stack trace element that has already been stored in the database
  * @param id id of this stack trace element in the database
  * @param data Wrapped stack trace element data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElementRecord(id: Int, data: StackTraceElementRecordData)
	extends StoredModelConvertible[StackTraceElementRecordData]

