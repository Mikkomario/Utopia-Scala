package utopia.logos.database.factory.word

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosTables
import utopia.logos.model.partial.word.StatementData
import utopia.logos.model.stored.word.Statement
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading statement data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object StatementDbFactory 
	extends FromValidatedRowModelFactory[Statement] with FromRowFactoryWithTimestamps[Statement]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = LogosTables.statement
	
	override protected def fromValidatedModel(valid: Model) = 
		Statement(valid("id").getInt, StatementData(valid("delimiterId").int, valid("created").getInstant))
}

