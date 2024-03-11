package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.Statement

/**
  * Used for reading statement data from the DB
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object StatementFactory 
	extends FromValidatedRowModelFactory[Statement] with FromRowFactoryWithTimestamps[Statement]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = EmissaryTables.statement
	
	override protected def fromValidatedModel(valid: Model) = 
		Statement(valid("id").getInt, StatementData(valid("delimiterId").int, valid("created").getInstant))
}

