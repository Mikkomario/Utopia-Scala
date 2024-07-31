package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.{OrganizationDeletionCancellationFactory, OrganizationDeletionWithCancellationsFactory}
import utopia.metropolis.model.combined.organization.OrganizationDeletionWithCancellations
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

import java.time.Instant

object ManyOrganizationDeletionsWithCancellationsAccess 
	extends ViewFactory[ManyOrganizationDeletionsWithCancellationsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyOrganizationDeletionsWithCancellationsAccess = 
		new _ManyOrganizationDeletionsWithCancellationsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyOrganizationDeletionsWithCancellationsAccess(condition: Condition) 
		extends ManyOrganizationDeletionsWithCancellationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return multiple organization deletions at a time, 
  * including cancellation data
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyOrganizationDeletionsWithCancellationsAccess 
	extends ManyOrganizationDeletionsAccessLike[OrganizationDeletionWithCancellations, 
		ManyOrganizationDeletionsWithCancellationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to organization deletions that haven't been cancelled yet
	  */
	def notCancelled = filter(factory.notLinkedCondition)
	
	/**
	  * An access point to organization deletions that have been cancelled
	  */
	def cancelled = filter(factory.isLinkedCondition)
	
	/**
	  * Factory used for reading cancellation data
	  */
	protected def cancellationFactory = OrganizationDeletionCancellationFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionWithCancellationsFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyOrganizationDeletionsWithCancellationsAccess = 
		ManyOrganizationDeletionsWithCancellationsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param threshold A time threshold
	  * @return An access point to organization deletions that were cancelled before the specified threshold
	  */
	def cancelledBefore(threshold: Instant) = filter(cancellationFactory.beforeCondition(threshold))
}

