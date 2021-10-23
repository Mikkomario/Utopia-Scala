package utopia.citadel.database.factory.organization

import utopia.citadel.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.organization.DeletionCancelData
import utopia.metropolis.model.stored.organization.DeletionCancel
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading organization deletion cancellations from DB
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.0
  */
@deprecated("Replaced with OrganizationDeletionCancellationFactory", "v2.0")
object DeletionCancelFactory extends FromValidatedRowModelFactory[DeletionCancel]
	with FromRowFactoryWithTimestamps[DeletionCancel]
{
	// IMPLEMENTED	-------------------------------
	
	override def table = Tables.organizationDeletionCancellation
	
	override def creationTimePropertyName = "created"
	
	override protected def fromValidatedModel(model: Model[Constant]) = DeletionCancel(model("id"),
		DeletionCancelData(model("deletionId"), model("creatorId"), model(creationTimePropertyName)))
}
