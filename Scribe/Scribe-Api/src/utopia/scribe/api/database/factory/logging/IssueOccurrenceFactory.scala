package utopia.scribe.api.database.factory.logging

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading issue occurrence data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueOccurrenceFactory extends FromValidatedRowModelFactory[IssueOccurrence]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ScribeTables.issueOccurrence
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueOccurrence(valid("id").getInt, IssueOccurrenceData(valid("caseId").getInt, 
			valid("errorMessages").notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString }; case None => Vector.empty }, 
			valid("details").notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }, 
			valid("count").getInt, Span(valid("firstOccurrence").getInstant, 
			valid("lastOccurrence").getInstant)))
}

