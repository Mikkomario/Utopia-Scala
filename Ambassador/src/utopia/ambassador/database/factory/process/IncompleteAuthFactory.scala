package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.IncompleteAuthModel
import utopia.ambassador.model.partial.process.IncompleteAuthData
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading incomplete authentication cases from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object IncompleteAuthFactory extends FromValidatedRowModelFactory[IncompleteAuth] with Deprecatable
{
	// COMPUTED -------------------------------
	
	private def model = IncompleteAuthModel
	
	
	// IMPLEMENTED  ---------------------------
	
	override def table = AmbassadorTables.incompleteAuth
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model[Constant]) = IncompleteAuth(model("id"),
		IncompleteAuthData(model("serviceId"), model("code"), model("token"), model("expiration"),
			model("created")))
}
