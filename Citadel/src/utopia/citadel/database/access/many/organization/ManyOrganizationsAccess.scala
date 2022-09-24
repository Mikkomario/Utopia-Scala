package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.access.many.description.{DbOrganizationDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.OrganizationFactory
import utopia.citadel.database.model.organization.OrganizationModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedOrganization
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyOrganizationsAccess
{
	// NESTED	--------------------
	
	private class ManyOrganizationsSubView(override val parent: ManyRowModelAccess[Organization], 
		override val filterCondition: Condition) 
		extends ManyOrganizationsAccess with SubView
}

/**
  * A common trait for access points which target multiple Organizations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyOrganizationsAccess 
	extends ManyRowModelAccess[Organization] with ManyDescribedAccess[Organization, DescribedOrganization]
		with FilterableView[ManyOrganizationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * creatorIds of the accessible Organizations
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible Organizations
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationFactory
	
	override protected def describedFactory = DescribedOrganization
	
	override protected def manyDescriptionsAccess = DbOrganizationDescriptions
	
	override def filter(additionalCondition: Condition): ManyOrganizationsAccess = 
		new ManyOrganizationsAccess.ManyOrganizationsSubView(this, additionalCondition)
	
	override def idOf(item: Organization) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Organization instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Organization instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted Organization instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any Organization instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
}

