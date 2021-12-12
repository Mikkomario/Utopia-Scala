package utopia.keep.database.factory.logging

import utopia.flow.datastructure.immutable.Model
import utopia.keep.database.KeepTables
import utopia.keep.model.partial.logging.ProblemCaseData
import utopia.keep.model.stored.logging.ProblemCase
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading ProblemCase data from the DB
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object ProblemCaseFactory 
	extends FromValidatedRowModelFactory[ProblemCase] with FromRowFactoryWithTimestamps[ProblemCase]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = KeepTables.problemCase
	
	override def fromValidatedModel(valid: Model) = 
		ProblemCase(valid("id").getInt, ProblemCaseData(valid("problemId").getInt, valid("details").string, 
			valid("stack").string, valid("created").getInstant))
}

