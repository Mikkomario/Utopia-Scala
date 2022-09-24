package utopia.ambassador.controller.template

import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.exodus.util.ExodusContext.logger
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.collection.CollectionExtensions._
import utopia.vault.database.Connection

import java.net.URLEncoder
import scala.io.Codec
import scala.util.Try

/**
  * A common trait for authentication redirection implementations that may be service-specific
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
trait AuthRedirector
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Encoding to use for query parameter values. None if no encoding is required.
	  */
	def parameterEncoding: Option[Codec]
	
	/**
	  * Specifies optional query parameters for the redirection.
	  * Default parameters are: response_type, redirect_uri, client_id and state
	  * @param settings Service-specific settings from the database
	  * @param preparation Redirect preparation
	  * @param scopes Scopes to request
	  * @param connection Implicit database connection
	  * @return Parameters to add to the default parameters
	  */
	def extraParametersFor(settings: AuthServiceSettings, preparation: AuthPreparation, scopes: Vector[Scope])
	                      (implicit connection: Connection): Model
	
	
	// OTHER    -------------------------------
	
	/**
	  * Determines the complete redirection url with the specified settings
	  * @param state Client state (token)
	  * @param settings Service settings to use
	  * @param preparation Authentication preparation
	  * @param scopes Requested scopes
	  * @param connection Implicit DB Connection
	  * @return Complete redirect url
	  */
	def redirectionFor(state: String, settings: AuthServiceSettings, preparation: AuthPreparation, scopes: Vector[Scope])
	                  (implicit connection: Connection) =
	{
		// Determines the parameters to include
		val defaultParameters = Model(Vector("response_type" -> "code", "redirect_uri" -> settings.redirectUrl,
			"client_id" -> settings.clientId, "state" -> state))
		val extraParameters = extraParametersFor(settings, preparation, scopes)
		val allAttributes = defaultParameters.attributesWithValue ++ extraParameters.attributesWithValue
		
		// Creates a query parameters string
		val attributes = parameterEncoding match
		{
			// Case: Encoding is used
			case Some(codec) =>
				val codecName = codec.charSet.name()
				Try {
					allAttributes.map { att => att.name -> URLEncoder.encode(att.value.getString, codecName) }
				}.getOrMap { error =>
					// If encoding fails, defaults to no encoding
					logger(error, s"Failed to encode oauth query parameters to $codecName")
					allAttributes.map { att => att.name -> att.value.getString }
				}
			// Case: No encoding is used
			case None => allAttributes.map { att => att.name -> att.value.getString }
		}
		
		s"${settings.authenticationUrl}?${attributes.map { case (key, value) => s"$key=$value" }}"
	}
}
