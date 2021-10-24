package utopia.citadel.database

import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object CitadelTables
{
	// ATTRIBUTES   ----------------
	
	/**
	  * Table that contains ClientDeviceDescriptions (Links ClientDevices with their descriptions)
	  */
	lazy val clientDeviceDescription = DescriptionLinkTable(apply("client_device_description"), "deviceId")
	/**
	  * Table that contains DescriptionRoleDescriptions (Links DescriptionRoles with their descriptions)
	  */
	lazy val descriptionRoleDescription = DescriptionLinkTable(apply("description_role_description"), "roleId")
	/**
	  * Table that contains LanguageDescriptions (Links Languages with their descriptions)
	  */
	lazy val languageDescription = DescriptionLinkTable(apply("language_description"), "languageId")
	/**
	  * Table that contains LanguageFamiliarityDescriptions (Links LanguageFamiliarities
		with their descriptions)
	  */
	lazy val languageFamiliarityDescription =
		DescriptionLinkTable(apply("language_familiarity_description"), "familiarityId")
	/**
	  * Table that contains OrganizationDescriptions (Links Organizations with their descriptions)
	  */
	lazy val organizationDescription = DescriptionLinkTable(apply("organization_description"), "organizationId")
	/**
	  * Table that contains TaskDescriptions (Links Tasks with their descriptions)
	  */
	lazy val taskDescription = DescriptionLinkTable(apply("task_description"), "taskId")
	/**
	  * Table that contains UserRoleDescriptions (Links UserRoles with their descriptions)
	  */
	lazy val userRoleDescription = DescriptionLinkTable(apply("user_role_description"), "roleId")
	
	
	// COMPUTED	--------------------
	
	/**
	  * Table that contains ClientDevices (Represents a device (e.g. a browser or a computer) a user uses to interact 
		with this service)
	  */
	def clientDevice = apply("client_device")
	
	/**
	  * Table that contains ClientDeviceUsers (Links users to the devices they are using)
	  */
	def clientDeviceUser = apply("client_device_user")
	
	/**
	  * Table that contains Descriptions (Represents some description of some item in some language)
	  */
	def description = apply("description")
	
	/**
	  * Table that contains DescriptionRoles (An enumeration for different roles or purposes a description can serve)
	  */
	def descriptionRole = apply("description_role")
	
	/**
	  * Table that contains Invitations (Represents an invitation to join an organization)
	  */
	def invitation = apply("invitation")
	
	/**
	  * Table that contains InvitationResponses (Represents a response (yes|no) to an invitation to join an organization)
	  */
	def invitationResponse = apply("invitation_response")
	
	/**
	  * Table that contains Languages (Represents a language)
	  */
	def language = apply("language")
	
	/**
	  * Table that contains LanguageFamiliarities (Represents a language skill level)
	  */
	def languageFamiliarity = apply("language_familiarity")
	
	/**
	  * Table that contains MemberRoles (Links an organization membership to the roles that member has within that organization)
	  */
	def memberRoleLink = apply("member_role")
	
	/**
	  * Table that contains Memberships (Lists organization members, including membership history)
	  */
	def membership = apply("membership")
	
	/**
	  * Table that contains Organizations (Represents an organization or a user group)
	  */
	def organization = apply("organization")
	
	/**
	  * Table that contains OrganizationDeletions (Represents a request to delete an organization. There exists a time period between the request and its completion, 
		during which other users may cancel the deletion.)
	  */
	def organizationDeletion = apply("organization_deletion")
	
	/**
	  * Table that contains OrganizationDeletionCancellations (Records a cancellation for a pending organization deletion request)
	  */
	def organizationDeletionCancellation = apply("organization_deletion_cancellation")
	
	/**
	  * Table that contains Tasks (Represents a type of task a user can perform (within an organization))
	  */
	def task = apply("task")
	
	/**
	  * Table that contains Users (Represents a program user)
	  */
	def user = apply("user")
	
	/**
	  * Table that contains UserLanguages (Links user with their language familiarity levels)
	  */
	def userLanguageLink = apply("user_language")
	
	/**
	  * Table that contains UserRoles (An enumeration for different roles a user may have within an organization)
	  */
	def userRole = apply("user_role")
	
	/**
	  * Table that contains UserRoleRights (Used for listing / linking, 
		which tasks different organization membership roles allow)
	  */
	def userRoleRight = apply("user_role_right")
	
	/**
	  * Table that contains UserSettings (Versioned user-specific settings)
	  */
	def userSettings = apply("user_settings")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = Tables(tableName)
}

