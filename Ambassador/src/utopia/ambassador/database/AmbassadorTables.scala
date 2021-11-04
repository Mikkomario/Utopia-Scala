package utopia.ambassador.database

import utopia.citadel.database.Tables
import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AmbassadorTables
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Table that contains ScopeDescriptions (Links Scopes with their descriptions)
	  */
	lazy val scopeDescription = DescriptionLinkTable(apply("scope_description"), "scopeId")
	
	
	// COMPUTED	--------------------
	
	/**
	  * Table that contains AuthCompletionRedirectTargets (Used for storing client-given rules for redirecting the user after the OAuth process completion. Given during the OAuth preparation.)
	  */
	def authCompletionRedirectTarget = apply("oauth_completion_redirect_target")
	
	/**
	  * Table that contains AuthPreparations (Used for preparing and authenticating an OAuth process that follows)
	  */
	def authPreparation = apply("oauth_preparation")
	
	/**
	  * Table that contains AuthPreparationScopeLinks (Links a requested scope to an OAuth preparation)
	  */
	def authPreparationScopeLink = apply("oauth_preparation_scope")
	
	/**
	  * Table that contains AuthRedirects (Records each event when a user is directed to the 3rd party OAuth service. These close the linked preparations.)
	  */
	def authRedirect = apply("oauth_redirect")
	
	/**
	  * Table that contains AuthRedirectResults (Records the cases when the user arrives back from the 3rd party OAuth service, 
		whether the authentication succeeded or not.)
	  */
	def authRedirectResult = apply("oauth_redirect_result")
	
	/**
	  * Table that contains AuthServices (Represents a service that provides an OAuth interface (e.g. Google))
	  */
	def authService = apply("oauth_service")
	
	/**
	  * Table that contains AuthServiceSettings (Specifies service-specific settings. It is recommended to have only one instance per service.)
	  */
	def authServiceSettings = apply("oauth_service_settings")
	
	/**
	  * Table that contains AuthTokens (Tokens (both access and refresh) used for authenticating 3rd party requests)
	  */
	def authToken = apply("oauth_token")
	
	/**
	  * Table that contains AuthTokenScopeLinks (Used for listing, 
		which scopes are available based on which authentication token)
	  */
	def authTokenScopeLink = apply("oauth_token_scope")
	
	/**
	  * Table that contains IncompleteAuths (Represents a case where a user arrives from a 3rd party service without first preparing an authentication on this side)
	  */
	def incompleteAuth = apply("incomplete_oauth")
	
	/**
	  * Table that contains IncompleteAuthLogins (Records cases where incomplete authentications are completed 
		with the user logging in)
	  */
	def incompleteAuthLogin = apply("incomplete_oauth_login")
	
	/**
	  * Table that contains Scopes (Scopes are like access rights which can be requested from 3rd party services. They determine what the application is allowed to do in behalf of the user.)
	  */
	def scope = apply("oauth_scope")
	
	/**
	  * Table that contains TaskScopeLinks (Links tasks with the scopes that are required to perform them)
	  */
	def taskScopeLink = apply("task_scope")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = Tables(tableName)
}

