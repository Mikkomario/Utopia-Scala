package utopia.citadel.database.factory.organization

import utopia.citadel.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.organization.DeletionData
import utopia.metropolis.model.stored.organization.Deletion
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading organization deletions (without cancellations) from the DB
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.0
  */
@deprecated("Replaced with OrganizationDeletionFactory", "v2.0")
object DeletionFactory extends FromValidatedRowModelFactory[Deletion]
{
	override def table = Tables.organizationDeletion
	
	override protected def fromValidatedModel(model: Model) =
		Deletion(model("id"), DeletionData(model("organizationId"), model("creatorId"), model("actualization")))
}
