package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.partial.process.AuthRedirectData
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading authentication user redirection events from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthRedirectFactory extends FromValidatedRowModelFactory[AuthRedirect] with Deprecatable
{
	// COMPUTED --------------------------------
	
	private def model = AuthRedirectModel
	
	
	// IMPLEMENTED  ----------------------------
	
	override def table = AmbassadorTables.authRedirect
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model[Constant]) = AuthRedirect(model("id"),
		AuthRedirectData(model("preparationId"), model("token"), model("expiration"), model("created")))
}
