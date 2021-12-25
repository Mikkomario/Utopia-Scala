package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.organization.ManyOrganizationDeletionsWithCancellationsAccess.SubAccess
import utopia.citadel.database.factory.organization.{OrganizationDeletionCancellationFactory, OrganizationDeletionWithCancellationsFactory}
import utopia.metropolis.model.combined.organization.OrganizationDeletionWithCancellations
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyOrganizationDeletionsWithCancellationsAccess
{
	private class SubAccess(override val parent: ManyModelAccess[OrganizationDeletionWithCancellations],
	                        override val filterCondition: Condition)
		extends ManyOrganizationDeletionsWithCancellationsAccess with SubView
}

/**
  * A common trait for access points that return multiple organization deletions at a time, including cancellation data
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyOrganizationDeletionsWithCancellationsAccess
	extends ManyOrganizationDeletionsAccessLike[OrganizationDeletionWithCancellations,
		ManyOrganizationDeletionsWithCancellationsAccess]
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return Factory used for reading cancellation data
	  */
	protected def cancellationFactory =
		OrganizationDeletionCancellationFactory
	
	/**
	  * @return An access point to organization deletions that haven't been cancelled yet
	  */
	def notCancelled = filter(factory.notLinkedCondition)
	/**
	  * @return An access point to organization deletions that have been cancelled
	  */
	def cancelled = filter(factory.isLinkedCondition)
	
	
	// IMPLEMENTED  ------------------------
	
	override def factory = OrganizationDeletionWithCancellationsFactory
	
	override protected def _filter(condition: Condition): ManyOrganizationDeletionsWithCancellationsAccess =
		new SubAccess(this, condition)
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param threshold A time threshold
	  * @return An access point to organization deletions that were cancelled before the specified threshold
	  */
	def cancelledBefore(threshold: Instant) = filter(cancellationFactory.createdBeforeCondition(threshold))
}
