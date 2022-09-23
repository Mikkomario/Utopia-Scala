package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.process.AuthRedirectResultData
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.flow.collection.value.typeless.Model
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading AuthRedirectResult data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthRedirectResultFactory 
	extends FromValidatedRowModelFactory[AuthRedirectResult] 
		with FromRowFactoryWithTimestamps[AuthRedirectResult]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = AmbassadorTables.authRedirectResult
	
	override def fromValidatedModel(valid: Model) =
		AuthRedirectResult(valid("id").getInt, AuthRedirectResultData(valid("redirectId").getInt, 
			valid("didReceiveCode").getBoolean, valid("didReceiveToken").getBoolean, 
			valid("created").getInstant))
}

