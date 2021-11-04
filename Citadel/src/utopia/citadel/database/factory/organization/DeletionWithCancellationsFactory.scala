package utopia.citadel.database.factory.organization

import utopia.metropolis.model.combined.organization.DeletionWithCancellations
import utopia.metropolis.model.stored.organization.{Deletion, DeletionCancel}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory

/**
  * Used for reading organization deletion attempts from DB
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.0
  */
@deprecated("Replaced with OrganizationDeletionWithCancellationsFactory", "v2.0")
object DeletionWithCancellationsFactory
	extends MultiCombiningFactory[DeletionWithCancellations, Deletion, DeletionCancel]
{
	// IMPLEMENTED  ----------------------------
	
	override def isAlwaysLinked = false
	
	override def parentFactory = DeletionFactory
	
	override def childFactory = DeletionCancelFactory
	
	override def apply(parent: Deletion, children: Vector[DeletionCancel]) =
		DeletionWithCancellations(parent, children)
}
