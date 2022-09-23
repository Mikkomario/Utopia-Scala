package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.ScopeData
import utopia.exodus.model.stored.auth.Scope
import utopia.flow.collection.value.typeless.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading scope data from the DB
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object ScopeFactory extends FromValidatedRowModelFactory[Scope]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.scope
	
	override def fromValidatedModel(valid: Model) = 
		Scope(valid("id").getInt, ScopeData(valid("name").getString, valid("created").getInstant))
}

