package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading authentication preparation data from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthPreparationFactory extends FromValidatedRowModelFactory[AuthPreparation]
	with FromRowFactoryWithTimestamps[AuthPreparation]
{
	override val creationTimePropertyName = "created"
	
	override def table = AmbassadorTables.authPreparation
	
	override protected def fromValidatedModel(model: Model[Constant]) = AuthPreparation(model("id"),
		AuthPreparationData(model("userId"), model("token"), model("clientState"), model("created")))
}
