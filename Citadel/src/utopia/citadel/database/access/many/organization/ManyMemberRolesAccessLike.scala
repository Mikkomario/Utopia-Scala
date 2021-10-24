package utopia.citadel.database.access.many.organization

import utopia.citadel.database.model.organization.MemberRoleModel
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

/**
  * A common trait for access points which target multiple MemberRoles or similar instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMemberRolesAccessLike[+A, +Repr <: ManyModelAccess[A]]
	extends ManyModelAccess[A] with Indexed
{
	// ABSTRACT --------------------
	
	protected def _filter(additionalCondition: Condition): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * membershipIds of the accessible MemberRoles
	  */
	def membershipIds(implicit connection: Connection) = 
		pullColumn(model.membershipIdColumn).flatMap { value => value.int }
	/**
	  * roleIds of the accessible MemberRoles
	  */
	def roleIds(implicit connection: Connection) = pullColumn(model.roleIdColumn)
		.flatMap { value => value.int }
	/**
	  * creatorIds of the accessible MemberRoles
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	/**
	  * creationTimes of the accessible MemberRoles
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * deprecationTimes of the accessible MemberRoles
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MemberRoleModel
	
	
	// IMPLEMENTED  ----------------
	
	override def filter(additionalCondition: Condition) = _filter(additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Deprecates all accessible member roles
	  * @param connection Implicit DB Connection
	  * @return Whether any role links were affected
	  */
	def deprecate()(implicit connection: Connection) = deprecationTimes = Now
	
	/**
	  * @param membershipId A membership id
	  * @return An access point to membership-role-links concerning that membership
	  */
	def withMembershipId(membershipId: Int) =
		filter(model.withMembershipId(membershipId).toCondition)
	/**
	  * @param userRoleId A user role id
	  * @return An access point to membership-role-links with that role
	  */
	def withRoleId(userRoleId: Int) = filter(model.withRoleId(userRoleId).toCondition)
	/**
	  * @param userRoleIds Ids of the targeted user roles
	  * @return An access point to membership-role-links to those roles
	  */
	def withAnyOfRoles(userRoleIds: Iterable[Int]) = filter(model.roleIdColumn in userRoleIds)
	
	/**
	  * @param userRoleId Id of the searched user role
	  * @param connection Implicit DB Connection
	  * @return Whether this access point includes connections to that user role
	  */
	def containsRoleWithId(userRoleId: Int)(implicit connection: Connection) =
		exists(model.withRoleId(userRoleId).toCondition)
	
	/**
	  * Updates the created of the targeted MemberRole instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the creatorId of the targeted MemberRole instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	/**
	  * Updates the deprecatedAfter of the targeted MemberRole instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	/**
	  * Updates the membershipId of the targeted MemberRole instance(s)
	  * @param newMembershipId A new membershipId to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def membershipIds_=(newMembershipId: Int)(implicit connection: Connection) = 
		putColumn(model.membershipIdColumn, newMembershipId)
	/**
	  * Updates the roleId of the targeted MemberRole instance(s)
	  * @param newRoleId A new roleId to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def roleIds_=(newRoleId: Int)(implicit connection: Connection) = putColumn(model.roleIdColumn, newRoleId)
}

