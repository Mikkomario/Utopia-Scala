package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading authentication preparation data from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthPreparationFactory extends FromValidatedRowModelFactory[AuthPreparation] with Deprecatable
{
	// COMPUTED ----------------------------
	
	private def model = AuthPreparationModel
	
	
	// IMPLEMENTED  ------------------------
	
	override def table = AmbassadorTables.authPreparation
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model[Constant]) = AuthPreparation(model("id"),
		AuthPreparationData(model("userId"), model("token"), model("expiration"), model("clientState"),
			model("created")))
}
