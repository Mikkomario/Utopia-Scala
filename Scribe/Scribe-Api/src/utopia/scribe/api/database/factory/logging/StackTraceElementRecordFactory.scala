package utopia.scribe.api.database.factory.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading stack trace element record data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object StackTraceElementRecordFactory extends FromValidatedRowModelFactory[StackTraceElementRecord]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ScribeTables.stackTraceElementRecord
	
	override protected def fromValidatedModel(valid: Model) = 
		StackTraceElementRecord(valid("id").getInt, StackTraceElementRecordData(valid("fileName").getString, 
			valid("className").getString, valid("methodName").getString, valid("lineNumber").int, 
			valid("causeId").int))
}

