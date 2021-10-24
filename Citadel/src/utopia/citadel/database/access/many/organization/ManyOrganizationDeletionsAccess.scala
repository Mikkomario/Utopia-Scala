package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.OrganizationDeletionFactory
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyOrganizationDeletionsAccess
{
	// NESTED	--------------------
	
	private class ManyOrganizationDeletionsSubView(override val parent: ManyRowModelAccess[OrganizationDeletion], 
		override val filterCondition: Condition) 
		extends ManyOrganizationDeletionsAccess with SubView
}

/**
  * A common trait for access points which target multiple OrganizationDeletions at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyOrganizationDeletionsAccess
	extends ManyOrganizationDeletionsAccessLike[OrganizationDeletion, ManyOrganizationDeletionsAccess] with
		ManyRowModelAccess[OrganizationDeletion] with Indexed
{
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def _filter(additionalCondition: Condition): ManyOrganizationDeletionsAccess =
		new ManyOrganizationDeletionsAccess.ManyOrganizationDeletionsSubView(this, additionalCondition)
}

