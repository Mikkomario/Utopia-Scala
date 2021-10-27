package utopia.ambassador.database.factory.token

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading AuthToken data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthTokenFactory 
	extends FromValidatedRowModelFactory[AuthToken] with FromRowFactoryWithTimestamps[AuthToken] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = AuthTokenModel.nonDeprecatedCondition
	
	override def table = AmbassadorTables.authToken
	
	override def fromValidatedModel(valid: Model) =
		AuthToken(valid("id").getInt, AuthTokenData(valid("userId").getInt, valid("token").getString, 
			valid("expires").instant, valid("created").getInstant, valid("deprecatedAfter").instant, 
			valid("isRefreshToken").getBoolean))
}

