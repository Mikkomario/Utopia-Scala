package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.process.IncompleteAuthLoginData
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading IncompleteAuthLogin data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object IncompleteAuthLoginFactory 
	extends FromValidatedRowModelFactory[IncompleteAuthLogin] 
		with FromRowFactoryWithTimestamps[IncompleteAuthLogin]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = AmbassadorTables.incompleteAuthLogin
	
	override def fromValidatedModel(valid: Model) =
		IncompleteAuthLogin(valid("id").getInt, IncompleteAuthLoginData(valid("authId").getInt, 
			valid("userId").getInt, valid("created").getInstant, valid("wasSuccess").getBoolean))
}

