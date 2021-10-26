package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.partial.process.AuthRedirectData
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading AuthRedirect data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthRedirectFactory 
	extends FromValidatedRowModelFactory[AuthRedirect] with FromRowFactoryWithTimestamps[AuthRedirect] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = AuthRedirectModel.nonDeprecatedCondition
	
	override def table = AmbassadorTables.authRedirect
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		AuthRedirect(valid("id").getInt, AuthRedirectData(valid("preparationId").getInt, 
			valid("token").getString, valid("expires").getInstant, valid("created").getInstant))
}

