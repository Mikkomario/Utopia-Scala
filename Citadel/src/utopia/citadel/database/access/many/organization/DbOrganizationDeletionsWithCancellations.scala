package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.{OrganizationDeletionCancellationFactory, OrganizationDeletionWithCancellationsFactory}
import utopia.flow.time.Now
import utopia.metropolis.model.combined.organization.OrganizationDeletionWithCancellations
import utopia.vault.nosql.view.{SubView, UnconditionalView}

import java.time.Instant

/**
  * Used for accessing multiple organization deletions at a time, including their cancellations
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object DbOrganizationDeletionsWithCancellations
	extends ManyOrganizationDeletionsAccessLike[OrganizationDeletionWithCancellations] with UnconditionalView
{
	// COMPUTED ----------------------------------
	
	private def cancellationFactory =
		OrganizationDeletionCancellationFactory
	
	/**
	  * @return An access point to organization deletions that haven't been cancelled yet
	  */
	def notCancelled = DbNotCancelledOrganizationDeletions
	/**
	  * @return An access point to organization deletions that have been cancelled
	  */
	def cancelled = filter(factory.isLinkedCondition)
	
	
	// IMPLEMENTED  ------------------------------
	
	override def factory = OrganizationDeletionWithCancellationsFactory
	
	override protected def defaultOrdering = Some(factory.parentFactory.defaultOrdering)
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param threshold A time threshold
	  * @return An access point to organization deletions that were cancelled before the specified threshold
	  */
	def cancelledBefore(threshold: Instant) =
		filter(cancellationFactory.createdBeforeCondition(threshold))
	
	
	// NESTED   ----------------------------------
	
	object DbNotCancelledOrganizationDeletions
		extends ManyOrganizationDeletionsAccessLike[OrganizationDeletionWithCancellations] with SubView
	{
		// ATTRIBUTES   --------------------------
		
		override lazy val filterCondition = factory.notLinkedCondition
		
		
		// COMPUTED ------------------------------
		
		/**
		  * @return An access point to deletions that are currently ready to actualize
		  */
		def readyToActualize = actualizingBefore(Now)
		
		
		// IMPLEMENTED  --------------------------
		
		override protected def parent =
			DbOrganizationDeletionsWithCancellations
		override def factory = parent.factory
		override protected def defaultOrdering = parent.defaultOrdering
	}
}
