package utopia.exodus.database.factory.organization

import utopia.exodus.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.combined.organization.DeletionWithCancellations
import utopia.metropolis.model.partial.organization.DeletionData
import utopia.metropolis.model.stored.organization.{Deletion, DeletionCancel}
import utopia.vault.nosql.factory.PossiblyMultiLinkedFactory

/**
  * Used for reading organization deletion attempts from DB
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  */
object DeletionFactory extends PossiblyMultiLinkedFactory[DeletionWithCancellations, DeletionCancel]
{
	override def childFactory = DeletionCancelFactory
	
	override def apply(id: Value, model: Model[Constant], children: Seq[DeletionCancel]) =
	{
		table.requirementDeclaration.validate(model).toTry.map { valid =>
			DeletionWithCancellations(Deletion(id, DeletionData(valid("organizationId"), valid("creatorId"),
				valid("actualization"))), children.toVector)
		}
	}
	
	override def table = Tables.organizationDeletion
}
