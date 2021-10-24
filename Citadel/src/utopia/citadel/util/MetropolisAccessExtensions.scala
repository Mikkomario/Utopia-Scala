package utopia.citadel.util

import utopia.citadel.database.access.single.description.{DbDescription, DbDescriptionRole}
import utopia.citadel.database.access.single.device.{DbClientDevice, DbClientDeviceUser}
import utopia.citadel.database.access.single.language.{DbLanguage, DbLanguageFamiliarity}
import utopia.citadel.database.access.single.organization.{DbInvitation, DbInvitationResponse, DbMemberRoleLink, DbMembership, DbOrganization, DbOrganizationDeletion, DbOrganizationDeletionCancellation, DbTask, DbUserRole, DbUserRoleRight}
import utopia.citadel.database.access.single.user.{DbUser, DbUserLanguageLink, DbUserSettings}
import utopia.metropolis.model.stored.description.{Description, DescriptionRole}
import utopia.metropolis.model.stored.device.{ClientDevice, ClientDeviceUser}
import utopia.metropolis.model.stored.language.{Language, LanguageFamiliarity}
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse, MemberRoleLink, Membership, Organization, OrganizationDeletion, OrganizationDeletionCancellation, Task, UserRole, UserRoleRight}
import utopia.metropolis.model.stored.user.{User, UserLanguageLink, UserSettings}

/**
 * Provides implicit extensions for easily accessing stored items in the database from the Metropolis project
 * @author Mikko Hilpinen
 * @since 12.10.2021, v1.3
 */
object MetropolisAccessExtensions
{
	implicit class AccessibleDescription(val a: Description) extends AnyVal
	{
		/**
		  * @return An access point to this description in the database
		  */
		def access = DbDescription(a.id)
		def languageAccess = DbLanguage(a.languageId)
		def roleAccess = DbDescriptionRole(a.roleId)
	}
	implicit class AccessibleDescriptionRole(val a: DescriptionRole) extends AnyVal
	{
		/**
		  * @return An access point to this description role in the database
		  */
		def access = DbDescriptionRole(a.id)
	}
	implicit class AccessibleClientDevice(val a: ClientDevice) extends AnyVal
	{
		/**
		  * @return An access point to this device in the database
		  */
		def access = DbClientDevice(a.id)
	}
	implicit class AccessibleClientDeviceUser(val a: ClientDeviceUser) extends AnyVal
	{
		/**
		  * @return An access point to this client device user link in the database
		  */
		def access = DbClientDeviceUser(a.id)
		def deviceAccess = DbClientDevice(a.deviceId)
		def userAccess = DbUser(a.userId)
	}
	implicit class AccessibleLanguage(val l: Language) extends AnyVal
	{
		/**
		 * @return An access point to this language's data in the database
		 */
		def access = DbLanguage(l.id)
	}
	implicit class AccessibleLanguageFamiliarity(val a: LanguageFamiliarity) extends AnyVal
	{
		/**
		  * @return An access point to this language familiarity in the database
		  */
		def access = DbLanguageFamiliarity(a.id)
	}
	implicit class AccessibleInvitation(val a: Invitation) extends AnyVal
	{
		/**
		  * @return An access point to this invitation in the database
		  */
		def access = DbInvitation(a.id)
		def organizationAccess = DbOrganization(a.organizationId)
		def startingRoleAccess = DbUserRole(a.startingRoleId)
	}
	implicit class AccessibleInvitationResponse(val a: InvitationResponse) extends AnyVal
	{
		/**
		  * @return An access point to this invitation response in the database
		  */
		def access = DbInvitationResponse(a.id)
		def invitationAccess = DbInvitation(a.invitationId)
	}
	implicit class AccessibleMemberRoleLink(val a: MemberRoleLink) extends AnyVal
	{
		/**
		  * @return An access point to this member role -link in the database
		  */
		def access = DbMemberRoleLink(a.id)
		def membershipAccess = DbMembership(a.membershipId)
		def roleAccess = DbUserRole(a.roleId)
	}
	implicit class AccessibleMembership(val a: Membership) extends AnyVal
	{
		/**
		  * @return An access point to this membership in the database
		  */
		def access = DbMembership(a.id)
		def organizationAccess = DbOrganization(a.organizationId)
		def userAccess = DbUser(a.userId)
	}
	implicit class AccessibleOrganization(val a: Organization) extends AnyVal
	{
		/**
		  * @return An access point to this organization in the database
		  */
		def access = DbOrganization(a.id)
	}
	implicit class AccessibleOrganizationDeletion(val a: OrganizationDeletion) extends AnyVal
	{
		/**
		  * @return An access point to this in the database
		  */
		def access = DbOrganizationDeletion(a.id)
		def organizationAccess = DbOrganization(a.organizationId)
	}
	implicit class AccessibleOrganizationDeletionCancellation(val a: OrganizationDeletionCancellation) extends AnyVal
	{
		/**
		  * @return An access point to this cancellation in the database
		  */
		def access = DbOrganizationDeletionCancellation(a.id)
		def deletionAccess = DbOrganizationDeletion(a.deletionId)
	}
	implicit class AccessibleTask(val a: Task) extends AnyVal
	{
		/**
		  * @return An access point to this task in the database
		  */
		def access = DbTask(a.id)
	}
	implicit class AccessibleUserRole(val a: UserRole) extends AnyVal
	{
		/**
		  * @return An access point to this user role in the database
		  */
		def access = DbUserRole(a.id)
	}
	implicit class AccessibleUserRoleRight(val a: UserRoleRight) extends AnyVal
	{
		/**
		  * @return An access point to this right in the database
		  */
		def access = DbUserRoleRight(a.id)
		def roleAccess = DbUserRole(a.roleId)
		def taskAccess = DbTask(a.taskId)
	}
	implicit class AccessibleUser(val u: User) extends AnyVal
	{
		/**
		  * @return An access point to this user's data in the database
		  */
		def access = DbUser(u.id)
	}
	implicit class AccessibleUserLanguageLink(val a: UserLanguageLink) extends AnyVal
	{
		/**
		  * @return An access point to this link in the database
		  */
		def access = DbUserLanguageLink(a.id)
		def userAccess = DbUser(a.userId)
		def languageAccess = DbLanguage(a.languageId)
		def familiarityAccess = DbLanguageFamiliarity(a.familiarityId)
	}
	implicit class AccessibleUserSettings(val a: UserSettings) extends AnyVal
	{
		/**
		  * @return An access point to these settings in the database
		  */
		def access = DbUserSettings(a.id)
		def userAccess = DbUser(a.userId)
	}
}
