package utopia.scribe.api.database.factory.logging

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading issue occurrence data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueOccurrenceFactory 
	extends FromValidatedRowModelFactory[IssueOccurrence] with FromRowFactoryWithTimestamps[IssueOccurrence]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = ScribeTables.issueOccurrence
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueOccurrence(valid("id").getInt, IssueOccurrenceData(valid("caseId").getInt, 
			valid("errorMessages").notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString }; case None => Vector.empty }, 
			valid("created").getInstant))
}

