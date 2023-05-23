package utopia.scribe.api.database.factory.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading issue data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueFactory extends FromValidatedRowModelFactory[Issue] with FromRowFactoryWithTimestamps[Issue]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = ScribeTables.issue
	
	override protected def fromValidatedModel(valid: Model) = 
		Issue(valid("id").getInt, IssueData(valid("context").getString, 
			Severity.fromValue(valid("severityLevel")), valid("created").getInstant))
}

