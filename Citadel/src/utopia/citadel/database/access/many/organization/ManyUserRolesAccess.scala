package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.access.many.description.{DbUserRoleDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.UserRoleFactory
import utopia.citadel.database.model.organization.UserRoleModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedUserRole
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyUserRolesAccess
{
	// NESTED	--------------------
	
	private class ManyUserRolesSubView(override val parent: ManyRowModelAccess[UserRole], 
		override val filterCondition: Condition) 
		extends ManyUserRolesAccess with SubView
}

/**
  * A common trait for access points which target multiple UserRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyUserRolesAccess 
	extends ManyRowModelAccess[UserRole] with ManyDescribedAccess[UserRole, DescribedUserRole]
{
	// COMPUTED	--------------------
	
	/**
	  * creationTimes of the accessible UserRoles
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleFactory
	
	override protected def defaultOrdering = None
	
	override protected def describedFactory = DescribedUserRole
	
	override protected def manyDescriptionsAccess = DbUserRoleDescriptions
	
	override def filter(additionalCondition: Condition): ManyUserRolesAccess = 
		new ManyUserRolesAccess.ManyUserRolesSubView(this, additionalCondition)
	
	override def idOf(item: UserRole) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserRole instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserRole instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

