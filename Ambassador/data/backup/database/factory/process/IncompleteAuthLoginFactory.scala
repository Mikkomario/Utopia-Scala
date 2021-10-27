package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.process.IncompleteAuthLoginData
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading incomplete authentication logins from the DB
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object IncompleteAuthLoginFactory extends FromValidatedRowModelFactory[IncompleteAuthLogin]
{
	override def table = AmbassadorTables.incompleteAuthLogin
	
	override protected def fromValidatedModel(model: Model) = IncompleteAuthLogin(model("id"),
		IncompleteAuthLoginData(model("authenticationId"), model("userId"), model("created"), model("wasSuccess")))
}
