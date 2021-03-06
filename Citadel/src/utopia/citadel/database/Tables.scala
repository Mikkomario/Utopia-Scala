package utopia.citadel.database

import utopia.vault.model.immutable.Table

/**
  * Used for accessing various tables in the Utopia Citadel project and all dependent projects
  * @author Mikko Hilpinen
  * @since 26.6.2021 v1.0
  */
object Tables
{
	import utopia.citadel.util.CitadelContext._
	
	// ATTRIBUTES	----------------------
	
	private lazy val access = new utopia.vault.database.Tables(connectionPool)
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return Table that contains descriptions of various things
	  */
	def description = apply("description")
	/**
	  * @return Table that contains description role enumeration values
	  */
	def descriptionRole = apply("description_role")
	/**
	  * @return Table that contains links between description roles and their descriptions
	  */
	def descriptionRoleDescription = apply("description_role_description")
	
	/**
	  * @return Table that contains registered languages
	  */
	def language = apply("language")
	/**
	  * @return Table that contains links between languages and their descriptions
	  */
	def languageDescription = apply("language_description")
	/**
	  * @return A table that contains language familiarity levels
	  */
	def languageFamiliarity = apply("language_familiarity")
	/**
	  * @return A table that contains links between language familliarity levels and language familiarities
	  */
	def languageFamiliarityDescription = apply("language_familiarity_description")
	
	/**
	  * @return Table that contains users
	  */
	def user = apply("user")
	/**
	  * @return Table for user settings
	  */
	def userSettings = apply("user_settings")
	/**
	  * @return Table that links users with languages
	  */
	def userLanguage = apply("user_language")
	
	/**
	  * @return Table that registers the devices the clients use
	  */
	def clientDevice = apply("client_device")
	/**
	  * @return Table that links users with the devices they are using
	  */
	def userDevice = apply("client_device_user")
	/**
	  * @return A table that contains links between devices and their descriptions
	  */
	def deviceDescription = apply("client_device_description")
	
	/**
	  * @return Table that contains organizations
	  */
	def organization = apply("organization")
	/**
	  * @return Table that contains links between organizations and their descriptions
	  */
	def organizationDescription = apply("organization_description")
	/**
	  * @return Contains attempted and pending organization deletions
	  */
	def organizationDeletion = apply("organization_deletion")
	/**
	  * @return Contains organization deletion cancellations
	  */
	def organizationDeletionCancellation = apply("organization_deletion_cancellation")
	
	/**
	  * @return Table that contains organization user memberships
	  */
	def organizationMembership = apply("organization_membership")
	/**
	  * @return Table that lists all user roles
	  */
	def userRole = apply("organization_user_role")
	/**
	  * @return Table that contains links between user roles and their descriptions
	  */
	def roleDescription = apply("user_role_description")
	/**
	  * @return Table that contains role links for organization memberships
	  */
	def organizationMemberRole = apply("organization_member_role")
	/**
	  * @return Table that contains links between user roles and the tasks they have access to
	  */
	def roleRight = apply("user_role_right")
	
	/**
	  * @return A table that lists all available tasks/rights
	  */
	def task = apply("task")
	/**
	  * @return A table that contains links between tasks and descriptions
	  */
	def taskDescription = apply("task_description")
	
	/**
	  * @return Table that contains sent organization join invitations
	  */
	def organizationInvitation = apply("organization_invitation")
	/**
	  * @return Table that contains responses to organization join invitations
	  */
	def invitationResponse = apply("invitation_response")
	
	
	// OTHER	-------------------------------
	
	/**
	 * @param databaseName Name of the used database
	 * @param tableName Name of the targeted table
	 * @return A cached table
	 */
	def apply(databaseName: String, tableName: String): Table = access(databaseName, tableName)
	
	/**
	  * @param tableName Name of targeted table
	  * @return a cached table
	  */
	def apply(tableName: String): Table = access(databaseName, tableName)
}
