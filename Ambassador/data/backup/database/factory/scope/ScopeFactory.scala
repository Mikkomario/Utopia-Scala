package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Indexed

/**
  * Used for reading scope data from the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object ScopeFactory extends FromValidatedRowModelFactory[Scope] with Indexed
{
	override def table = AmbassadorTables.scope
	
	override protected def fromValidatedModel(model: Model) = Scope(model("id"),
		ScopeData(model("serviceId"), model("serviceSideName"), model("clientSideName"), model("priority")))
}
