package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.access.many.description.{DbUserRoleDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.UserRoleFactory
import utopia.citadel.database.model.organization.UserRoleModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.DescribedUserRole
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
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
		with FilterableView[ManyUserRolesAccess]
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
	
	/**
	  * @param connection Implicit DB Connection
	  * @param languageIds Ids of the languages in which role descriptions are read
	  * @return Detailed copies of these user roles
	  */
	def detailed(implicit connection: Connection, languageIds: LanguageIds) = {
		// Reads described copies first, then attaches task link information
		val roles = described
		val rights = DbUserRoleRights.withAnyOfRoles(roles.map { _.id }).pull
		val taskIdsPerRoleId = rights.groupMap { _.roleId } { _.taskId }
		roles.map { role => role.withAllowedTaskIds(taskIdsPerRoleId.getOrElse(role.id, Set()).toSet) }
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = UserRoleFactory
	
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

