package utopia.citadel.database.factory.organization

import utopia.metropolis.model.combined.organization.OrganizationDeletionWithCancellations
import utopia.metropolis.model.stored.organization.{OrganizationDeletion, OrganizationDeletionCancellation}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory

/**
  * Used for reading OrganizationDeletionWithCancellationss from the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationDeletionWithCancellationsFactory 
	extends MultiCombiningFactory[OrganizationDeletionWithCancellations, OrganizationDeletion, 
		OrganizationDeletionCancellation]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = OrganizationDeletionCancellationFactory
	
	override def isAlwaysLinked = false
	
	override def parentFactory = OrganizationDeletionFactory
	
	override def apply(deletion: OrganizationDeletion, cancellations: Seq[OrganizationDeletionCancellation]) =
		OrganizationDeletionWithCancellations(deletion, cancellations)
}

