package utopia.ambassador.database.factory.token

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading authentication tokens from the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object AuthTokenFactory extends FromValidatedRowModelFactory[AuthToken]
	with FromRowFactoryWithTimestamps[AuthToken] with Deprecatable
{
	override val creationTimePropertyName = "created"
	
	override def table = AmbassadorTables.authToken
	
	override def nonDeprecatedCondition = AuthTokenModel.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model) = AuthToken(model("id"),
		AuthTokenData(model("userId"), model("token"), model(creationTimePropertyName), model("expiration"),
			model("deprecatedAfter"), model("isRefreshToken")))
}
