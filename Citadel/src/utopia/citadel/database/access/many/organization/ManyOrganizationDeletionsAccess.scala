package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.OrganizationDeletionFactory
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

object ManyOrganizationDeletionsAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyOrganizationDeletionsAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyOrganizationDeletionsAccess
}

/**
  * A common trait for access points which target multiple OrganizationDeletions at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyOrganizationDeletionsAccess 
	extends ManyOrganizationDeletionsAccessLike[OrganizationDeletion, ManyOrganizationDeletionsAccess] 
		with ManyRowModelAccess[OrganizationDeletion] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to organization deletions including their cancellations
	  */
	def withCancellations = {
		accessCondition match 
		{
			case Some(condition) => DbOrganizationDeletionsWithCancellations.filter(condition)
			case None => DbOrganizationDeletionsWithCancellations
		}
	}
	
	/**
	  * An access point to organization deletions that haven't been cancelled yet
	  */
	def notCancelled = withCancellations.notCancelled
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyOrganizationDeletionsAccess = 
		ManyOrganizationDeletionsAccess(condition)
}

