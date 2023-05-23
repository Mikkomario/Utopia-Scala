package utopia.scribe.api.database.factory.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.partial.logging.StackTraceElementData
import utopia.scribe.core.model.stored.logging.StackTraceElement
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading stack trace element data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object StackTraceElementFactory extends FromValidatedRowModelFactory[StackTraceElement]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ScribeTables.stackTraceElement
	
	override protected def fromValidatedModel(valid: Model) = 
		StackTraceElement(valid("id").getInt, StackTraceElementData(valid("className").getString, 
			valid("methodName").getString, valid("lineNumber").getInt, valid("causeId").int))
}

