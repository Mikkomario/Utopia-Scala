package utopia.ambassador.database.factory.service

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading service data from the DB
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
object AuthServiceFactory extends FromValidatedRowModelFactory[AuthService]
{
	override def table = AmbassadorTables.service
	
	override protected def fromValidatedModel(model: Model) =
		AuthService(model("id"), model("name"), model("created"))
}
