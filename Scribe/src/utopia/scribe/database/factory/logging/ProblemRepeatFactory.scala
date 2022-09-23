package utopia.scribe.database.factory.logging

import utopia.flow.collection.value.typeless.Model
import utopia.scribe.database.ScribeTables
import utopia.scribe.model.partial.logging.ProblemRepeatData
import utopia.scribe.model.stored.logging.ProblemRepeat
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading ProblemRepeat data from the DB
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object ProblemRepeatFactory 
	extends FromValidatedRowModelFactory[ProblemRepeat] with FromRowFactoryWithTimestamps[ProblemRepeat]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = ScribeTables.problemRepeat
	
	override def fromValidatedModel(valid: Model) = 
		ProblemRepeat(valid("id").getInt, ProblemRepeatData(valid("caseId").getInt, 
			valid("created").getInstant))
}

