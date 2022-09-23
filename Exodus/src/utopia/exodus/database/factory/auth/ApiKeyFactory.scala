package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.ApiKeyData
import utopia.exodus.model.stored.auth.ApiKey
import utopia.flow.collection.value.typeless.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading ApiKey data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object ApiKeyFactory extends FromValidatedRowModelFactory[ApiKey]
{
	// IMPLEMENTED	--------------------
	
	override def table = ExodusTables.apiKey
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		ApiKey(valid("id").getInt, ApiKeyData(valid("token").getString, valid("name").getString, 
			valid("created").getInstant))
}

