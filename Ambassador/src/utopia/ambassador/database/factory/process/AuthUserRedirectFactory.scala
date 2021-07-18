package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.AuthUserRedirectModel
import utopia.ambassador.model.partial.process.AuthUserRedirectData
import utopia.ambassador.model.stored.process.AuthUserRedirect
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading authentication user redirection events from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthUserRedirectFactory extends FromValidatedRowModelFactory[AuthUserRedirect] with Deprecatable
{
	// COMPUTED --------------------------------
	
	private def model = AuthUserRedirectModel
	
	
	// IMPLEMENTED  ----------------------------
	
	override def table = AmbassadorTables.authRedirect
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model[Constant]) = AuthUserRedirect(model("id"),
		AuthUserRedirectData(model("preparationId"), model("token"), model("expiration"), model("created")))
}
