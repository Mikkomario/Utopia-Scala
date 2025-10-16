package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.TokenTypeData
import utopia.exodus.model.stored.auth.TokenType
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.TimeUnit.Minute
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading token type data from the DB
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object TokenTypeFactory extends FromValidatedRowModelFactory[TokenType]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.tokenType
	
	override def fromValidatedModel(valid: Model) = 
		TokenType(valid("id").getInt, TokenTypeData(valid("name").getString, 
			valid("durationMinutes").long.map(Minute.apply),
			valid("refreshedTypeId").int, valid("created").getInstant, valid("isSingleUseOnly").getBoolean))
}

