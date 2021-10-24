package utopia.exodus.database.factory.user

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.ApiKeyData
import utopia.exodus.model.stored.ApiKey
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for accessing DB data concerning registered api keys
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
object ApiKeyFactory extends FromValidatedRowModelFactory[ApiKey]
{
	override protected def fromValidatedModel(model: Model[Constant]) = ApiKey(model("id"),
		ApiKeyData(model("key"), model("name"), model("created")))
	
	override def table = ExodusTables.apiKey
}
