package utopia.scribe.api.database.factory.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading error record data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object ErrorRecordFactory extends FromValidatedRowModelFactory[ErrorRecord]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ScribeTables.errorRecord
	
	override protected def fromValidatedModel(valid: Model) = 
		ErrorRecord(valid("id").getInt, ErrorRecordData(valid("exceptionType").getString, 
			valid("stackTraceId").getInt, valid("causeId").int))
}

