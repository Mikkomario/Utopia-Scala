package utopia.citadel.database.access.single.user

import utopia.citadel.database.access.many.description.DbOrganizationDescriptions
import utopia.citadel.database.access.many.device.DbClientDeviceUsers
import utopia.citadel.database.access.many.organization.{DbInvitations, DbMemberships, DbMembershipsWithRoles, DbOrganizations, DbUserRoles}
import utopia.citadel.database.access.many.user.DbUserLanguageLinks
import utopia.citadel.database.access.single.device.DbClientDeviceUser
import utopia.citadel.database.access.single.organization.DbMembership
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.UserRoleWithRights
import utopia.metropolis.model.combined.user.{MyOrganization, UserWithLinks}
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

import java.time.Instant

/**
  * An access point to individual Users, based on their id
  * @since 2021-10-23
  */
case class DbSingleUser(id: Int) extends UniqueUserAccess with SingleIntIdModelAccess[User]
{
	// COMPUTED --------------------------
	
	/**
	  * @return An access point to this user's current settings
	  */
	def settings = DbUserSettings.forUserWithId(id)
	
	/**
	  * @return An access point to this user's language links
	  */
	def languageLinks = DbUserLanguageLinks.withUserId(id)
	/**
	  * @return An access point to links between this user and the devices this user uses
	  */
	def deviceLinks = DbClientDeviceUsers.withUserId(id)
	/**
	  * @return An access point to this user's memberships
	  */
	def memberships = DbMemberships.ofUserWithId(id)
	/**
	  * @return An access point to this user's memberships, including role information
	  */
	def membershipsWithRoles = DbMembershipsWithRoles.ofUserWithId(id)
	/**
	  * @return An access point to this user's current and historical memberships
	  */
	def currentAndPastMemberships = DbMemberships.withHistory.ofUserWithId(id)
	/**
	  * @return An access point to this user's current and historical memberships, including current and historical
	  *         role assignments
	  */
	def currentAndPastMembershipsWithRoles =
		DbMemberships.withHistory.withRoles.ofUserWithId(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return An access point to this user's received invitations (both through id linking and email linking)
	  */
	def receivedInvitations(implicit connection: Connection) =
	{
		// Needs email address for the complete search
		settings.email match
		{
			case Some(email) => DbInvitations.forRecipient(id, email)
			case None => DbInvitations.forRecipientWithId(id)
		}
	}
	
	/**
	  * @param connection Implicit DB connection
	  * @return Ids of the languages known by this user (unordered)
	  */
	def knownLanguageIds(implicit connection: Connection) = languageLinks.languageIds.toSet
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the languages known by this user (ordered from most to least preferred)
	  */
	def languageIds(implicit connection: Connection) = LanguageIds(languageLinks.withFamiliarities.languageIds)
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the devices this user is currently using
	  */
	def deviceIds(implicit connection: Connection) = deviceLinks.deviceIds
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the organizations this user is a member of
	  */
	def organizationIds(implicit connection: Connection) = memberships.organizationIds.toSet
	
	/**
	  * @param connection Implicit DB Connection
	  * @return This user, including language links and linked device ids
	  */
	def withLinks(implicit connection: Connection) = settings.pull.map { settings =>
		UserWithLinks(settings, languageLinks.pull, deviceIds)
	}
	
	/**
	  * @param connection Implicit DB Connection
	  * @param languageIds Ids of the languages in which organization descriptions are read
	  * @return All of this user's organizations, including descriptions and role information
	  */
	def myOrganizations(implicit connection: Connection, languageIds: LanguageIds) =
	{
		// Reads all memberships, including role information
		val readMemberships = membershipsWithRoles.pull
		// Reads available task ids for each role
		val roleIds = readMemberships.flatMap { _.roleIds }.toSet
		val roleRights = DbUserRoles(roleIds).rights.pull
		val taskIdsPerRoleId = roleRights.groupMap { _.roleId } { _.taskId }
		// Reads organization information, including descriptions
		val organizationIds = readMemberships.map { _.organizationId }.toSet
		val organizations = DbOrganizations(organizationIds).described
		val organizationById = organizations.map { o => o.id -> o }.toMap
		// Combines read information
		readMemberships.map { membership =>
			MyOrganization(id, organizationById(membership.organizationId),
				membership.roleLinks.map { _.roleId }.map { roleId =>
					UserRoleWithRights(roleId, taskIdsPerRoleId.getOrElse(roleId, Set()).toSet) })
		}
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param organizationId Id of the targeted organization
	  * @return An access point to this user's membership in that organization
	  */
	def membershipInOrganizationWithId(organizationId: Int) =
		DbMembership.betweenOrganizationAndUser(organizationId, id)
	/**
	  * @param deviceId A device id
	  * @return This user's device use -link with that device
	  */
	def linkToDeviceWithId(deviceId: Int) = DbClientDeviceUser.linkBetween(deviceId, id)
	
	/**
	  * @param organizationId Id of targeted organization
	  * @param connection Implicit DB Connection
	  * @return Whether this user is a member of that organization
	  */
	def isMemberOfOrganizationWithId(organizationId: Int)(implicit connection: Connection) =
		membershipInOrganizationWithId(organizationId).nonEmpty
	/**
	  * @param organizationIds Ids of the targeted organizations
	  * @param connection Implicit DB Connection
	  * @return Whether this user is a member of any of those organizations
	  */
	def isMemberOfAnyOfOrganizations(organizationIds: Iterable[Int])(implicit connection: Connection) =
		memberships.inAnyOfOrganizations(organizationIds).nonEmpty
	
	/**
	  * @param userId Id of another user
	  * @param connection Implicit DB Connection
	  * @return Whether there exists an organization in which both of these users are members
	  */
	def sharesOrganizationWithUserWithId(userId: Int)(implicit connection: Connection) =
	{
		// Case: Checking against self
		if (userId == id)
			true
		// Case: Checking against another user
		else
		{
			// Needs to perform two queries to avoid double joining
			val otherUserOrganizationIds = DbSingleUser(userId).organizationIds
			if (otherUserOrganizationIds.isEmpty)
				false
			else
				isMemberOfAnyOfOrganizations(otherUserOrganizationIds)
		}
	}
	
	/**
	  * @param threshold A time threshold
	  * @param connection Implicit DB Connection
	  * @return Whether organizations or memberships related to this user have been modified since the
	  *         specified time threshold
	  */
	def myOrganizationsAreModifiedSince(threshold: Instant)(implicit connection: Connection) =
	{
		// Checks whether memberships or their roles have been altered, or if any organization descriptions
		// have been altered
		if (currentAndPastMembershipsWithRoles.isModifiedSince(threshold))
			true
		else
		{
			val readOrganizationIds = organizationIds
			readOrganizationIds.nonEmpty && DbOrganizationDescriptions(organizationIds).isModifiedSince(threshold)
		}
	}
	
	/**
	  * Removes the specified languages from this user's known languages
	  * @param languageIds Ids of the targeted languages
	  * @param connection Implicit DB Connection
	  */
	def forgetLanguagesWithIds(languageIds: Iterable[Int])(implicit connection: Connection) =
	{
		if (languageIds.nonEmpty)
			languageLinks.withAnyOfLanguages(languageIds).delete()
	}
}