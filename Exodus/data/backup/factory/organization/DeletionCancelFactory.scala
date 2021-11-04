package utopia.exodus.database.factory.organization

import utopia.exodus.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.organization.DeletionCancelData
import utopia.metropolis.model.stored.organization.DeletionCancel
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading organization deletion cancellations from DB
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object DeletionCancelFactory extends FromValidatedRowModelFactory[DeletionCancel]
{
	// IMPLEMENTED	-------------------------------
	
	override def table = Tables.organizationDeletionCancellation
	
	override protected def fromValidatedModel(model: Model) = DeletionCancel(model("id"),
		DeletionCancelData(model("deletionId"), model("creatorId")))
}
