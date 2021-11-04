package utopia.ambassador.database

import utopia.citadel.database.Tables

/**
  * Used for accessing database tables introduced in this module
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object AmbassadorTables
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return Table that lists target OAuth services
	  */
	def service = apply("oauth_service")
	/**
	  * @return Table that lists settings used for each OAuth service
	  */
	def serviceSettings = apply("oauth_service_settings")
	
	/**
	  * @return Table that lists access scopes / rights / levels
	  */
	def scope = apply("scope")
	/**
	  * @return Table that links descriptions with access scopes
	  */
	def scopeDescription = apply("scope_description")
	/**
	  * @return Table that links tasks with required access scopes
	  */
	def taskScope = apply("task_scope")
	
	/**
	  * @return Table that documents prepared authentications
	  */
	def authPreparation = apply("oauth_preparation")
	/**
	  * @return Table that links authentication preparations with requested scopes
	  */
	def scopeRequestPreparation = apply("scope_request_preparation")
	/**
	  * @return Table that lists where the client wants to send the user upon OAuth process completion
	  */
	def completionRedirectTarget = apply("oauth_completion_redirect_target")
	
	/**
	  * @return Table that records OAuth user redirects (to the OAuth service)
	  */
	def authRedirect = apply("oauth_user_redirect")
	/**
	  * @return Table that records OAuth process completions
	  */
	def authRedirectResult = apply("oauth_user_redirect_result")
	
	/**
	  * @return Table that stores acquired refresh and access tokens
	  */
	def authToken = apply("oauth_token")
	/**
	  * @return Table that lists the scopes available to each access token
	  */
	def authTokenScope = apply("oauth_token_scope")
	
	/**
	  * @return Table that records cases where a user arrives from OAuth service without being
	  *         authorized in this service first
	  */
	def incompleteAuth = apply("incomplete_authentication")
	/**
	  * @return Table that records cases where incomplete auth cases were resolved by logging in
	  */
	def incompleteAuthLogin = apply("incomplete_authentication_login")
	
	
	// OTHER    ----------------------------------
	
	private def apply(tableName: String) = Tables(tableName)
}
