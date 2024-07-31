package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.description.{DbUserRoleDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.UserRoleFactory
import utopia.citadel.database.model.organization.UserRoleModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.DescribedUserRole
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyUserRolesAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyUserRolesAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyUserRolesAccess
}

/**
  * A common trait for access points which target multiple UserRoles at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
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
	  * Detailed copies of these user roles
	  * @param connection Implicit DB Connection
	  * @param languageIds Ids of the languages in which role descriptions are read
	  */
	def detailed(implicit connection: Connection, languageIds: LanguageIds) = {
		// Reads described copies first, then attaches task link information
		val roles = described
		val rights = DbUserRoleRights.withAnyOfRoles(roles.map { _.id }).pull
		val taskIdsPerRoleId = rights.groupMap { _.roleId } { _.taskId }
		roles.map { role => role.withAllowedTaskIds(taskIdsPerRoleId.getOrElse(role.id, Set()).toSet) }
	}
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleFactory
	
	override protected def describedFactory = DescribedUserRole
	
	override protected def manyDescriptionsAccess = DbUserRoleDescriptions
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyUserRolesAccess = ManyUserRolesAccess(condition)
	
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

