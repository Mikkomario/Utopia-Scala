package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading AuthCompletionRedirectTarget data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthCompletionRedirectTargetFactory extends FromValidatedRowModelFactory[AuthCompletionRedirectTarget]
{
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.authCompletionRedirectTarget
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		AuthCompletionRedirectTarget(valid("id").getInt, 
			AuthCompletionRedirectTargetData(valid("preparationId").getInt, valid("url").getString, 
			valid("resultStateFilter").boolean, valid("isLimitedToDenials").getBoolean))
}

