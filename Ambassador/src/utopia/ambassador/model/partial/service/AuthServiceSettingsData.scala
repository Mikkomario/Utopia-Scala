package utopia.ambassador.model.partial.service

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import java.util.concurrent.TimeUnit

/**
  * Specifies service-specific settings. It is recommended to have only one instance per service.
  * @param serviceId Id of the described service
  * @param clientId Id of this client in the referenced service
  * @param clientSecret This application's password to the referenced service
  * @param authenticationUrl Url to the endpoint that receives users for the OAuth process
  * @param tokenUrl Url to the endpoint that provides refresh and session tokens
  * @param redirectUrl Url to the endpoint in this application which receives the user after they've completed the OAuth process
  * @param incompleteAuthRedirectUrl Url on the client side (front) that receives the user when they arrive from an OAuth process that was not initiated in this application. None if this use case is not supported.
  * @param defaultCompletionRedirectUrl Url on the client side (front) where the user will be redirected upon authentication completion. Used if no redirect urls were prepared by the client.
  * @param preparationTokenDuration Duration how long preparation tokens can be used after they're issued before they expire
  * @param redirectTokenDuration Duration how long redirect tokens can be used after they're issued before they expire
  * @param incompleteAuthTokenDuration Duration how long incomplete authentication tokens can be used after they're issued before they expire
  * @param DefaultSessionDuration Duration of this AuthServiceSettings
  * @param created Time when this AuthServiceSettings was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthServiceSettingsData(serviceId: Int, clientId: String, clientSecret: String, 
	authenticationUrl: String, tokenUrl: String, redirectUrl: String, 
	incompleteAuthRedirectUrl: Option[String] = None, defaultCompletionRedirectUrl: Option[String] = None, 
	preparationTokenDuration: FiniteDuration = 5.minutes, redirectTokenDuration: FiniteDuration = 15.minutes, 
	incompleteAuthTokenDuration: FiniteDuration = 30.minutes, 
	DefaultSessionDuration: FiniteDuration = 22.hours, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("service_id" -> serviceId, "client_id" -> clientId, "client_secret" -> clientSecret, 
			"authentication_url" -> authenticationUrl, "token_url" -> tokenUrl, 
			"redirect_url" -> redirectUrl, "incomplete_auth_redirect_url" -> incompleteAuthRedirectUrl, 
			"default_completion_redirect_url" -> defaultCompletionRedirectUrl, 
			"preparation_token_duration" -> preparationTokenDuration.toUnit(TimeUnit.MINUTES), 
			"redirect_token_duration" -> redirectTokenDuration.toUnit(TimeUnit.MINUTES), 
			"incomplete_auth_token_duration" -> incompleteAuthTokenDuration.toUnit(TimeUnit.MINUTES), 
			"default_session_duration" -> DefaultSessionDuration.toUnit(TimeUnit.MINUTES), 
			"created" -> created))
}

