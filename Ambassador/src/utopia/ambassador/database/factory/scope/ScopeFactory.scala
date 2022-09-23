package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.collection.value.typeless.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading Scope data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object ScopeFactory extends FromValidatedRowModelFactory[Scope]
{
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.scope
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		Scope(valid("id").getInt, ScopeData(valid("serviceId").getInt, valid("name").getString, 
			valid("priority").int, valid("created").getInstant))
}

