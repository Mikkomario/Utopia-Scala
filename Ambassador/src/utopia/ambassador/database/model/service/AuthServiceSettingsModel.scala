package utopia.ambassador.database.model.service

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import utopia.ambassador.database.factory.service.AuthServiceSettingsFactory
import utopia.ambassador.model.partial.service.AuthServiceSettingsData
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.util.concurrent.TimeUnit

/**
  * Used for constructing AuthServiceSettingsModel instances and for inserting AuthServiceSettingss to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthServiceSettingsModel 
	extends DataInserter[AuthServiceSettingsModel, AuthServiceSettings, AuthServiceSettingsData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthServiceSettings serviceId
	  */
	val serviceIdAttName = "serviceId"
	
	/**
	  * Name of the property that contains AuthServiceSettings clientId
	  */
	val clientIdAttName = "clientId"
	
	/**
	  * Name of the property that contains AuthServiceSettings clientSecret
	  */
	val clientSecretAttName = "clientSecret"
	
	/**
	  * Name of the property that contains AuthServiceSettings authenticationUrl
	  */
	val authenticationUrlAttName = "authenticationUrl"
	
	/**
	  * Name of the property that contains AuthServiceSettings tokenUrl
	  */
	val tokenUrlAttName = "tokenUrl"
	
	/**
	  * Name of the property that contains AuthServiceSettings redirectUrl
	  */
	val redirectUrlAttName = "redirectUrl"
	
	/**
	  * Name of the property that contains AuthServiceSettings incompleteAuthRedirectUrl
	  */
	val incompleteAuthRedirectUrlAttName = "incompleteAuthRedirectUrl"
	
	/**
	  * Name of the property that contains AuthServiceSettings defaultCompletionRedirectUrl
	  */
	val defaultCompletionRedirectUrlAttName = "defaultCompletionRedirectUrl"
	
	/**
	  * Name of the property that contains AuthServiceSettings preparationTokenDuration
	  */
	val preparationTokenDurationAttName = "preparationTokenDurationMinutes"
	
	/**
	  * Name of the property that contains AuthServiceSettings redirectTokenDuration
	  */
	val redirectTokenDurationAttName = "redirectTokenDurationMinutes"
	
	/**
	  * Name of the property that contains AuthServiceSettings incompleteAuthTokenDuration
	  */
	val incompleteAuthTokenDurationAttName = "incompleteAuthTokenDurationMinutes"
	
	/**
	  * Name of the property that contains AuthServiceSettings DefaultSessionDuration
	  */
	val defaultSessionDurationAttName = "defaultSessionDurationMinutes"
	
	/**
	  * Name of the property that contains AuthServiceSettings created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthServiceSettings serviceId
	  */
	def serviceIdColumn = table(serviceIdAttName)
	
	/**
	  * Column that contains AuthServiceSettings clientId
	  */
	def clientIdColumn = table(clientIdAttName)
	
	/**
	  * Column that contains AuthServiceSettings clientSecret
	  */
	def clientSecretColumn = table(clientSecretAttName)
	
	/**
	  * Column that contains AuthServiceSettings authenticationUrl
	  */
	def authenticationUrlColumn = table(authenticationUrlAttName)
	
	/**
	  * Column that contains AuthServiceSettings tokenUrl
	  */
	def tokenUrlColumn = table(tokenUrlAttName)
	
	/**
	  * Column that contains AuthServiceSettings redirectUrl
	  */
	def redirectUrlColumn = table(redirectUrlAttName)
	
	/**
	  * Column that contains AuthServiceSettings incompleteAuthRedirectUrl
	  */
	def incompleteAuthRedirectUrlColumn = table(incompleteAuthRedirectUrlAttName)
	
	/**
	  * Column that contains AuthServiceSettings defaultCompletionRedirectUrl
	  */
	def defaultCompletionRedirectUrlColumn = table(defaultCompletionRedirectUrlAttName)
	
	/**
	  * Column that contains AuthServiceSettings preparationTokenDuration
	  */
	def preparationTokenDurationColumn = table(preparationTokenDurationAttName)
	
	/**
	  * Column that contains AuthServiceSettings redirectTokenDuration
	  */
	def redirectTokenDurationColumn = table(redirectTokenDurationAttName)
	
	/**
	  * Column that contains AuthServiceSettings incompleteAuthTokenDuration
	  */
	def incompleteAuthTokenDurationColumn = table(incompleteAuthTokenDurationAttName)
	
	/**
	  * Column that contains AuthServiceSettings defaultSessionDuration
	  */
	def defaultSessionDurationColumn = table(defaultSessionDurationAttName)
	
	/**
	  * Column that contains AuthServiceSettings created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthServiceSettingsFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthServiceSettingsData) = 
		apply(None, Some(data.serviceId), Some(data.clientId), Some(data.clientSecret), 
			Some(data.authenticationUrl), Some(data.tokenUrl), Some(data.redirectUrl), 
			data.incompleteAuthRedirectUrl, data.defaultCompletionRedirectUrl, 
			Some(data.preparationTokenDuration), Some(data.redirectTokenDuration), 
			Some(data.incompleteAuthTokenDuration), Some(data.defaultSessionDuration), Some(data.created))
	
	override def complete(id: Value, data: AuthServiceSettingsData) = AuthServiceSettings(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param authenticationUrl Url to the endpoint that receives users for the OAuth process
	  * @return A model containing only the specified authenticationUrl
	  */
	def withAuthenticationUrl(authenticationUrl: String) = apply(authenticationUrl = Some(authenticationUrl))
	
	/**
	  * @param clientId Id of this client in the referenced service
	  * @return A model containing only the specified clientId
	  */
	def withClientId(clientId: String) = apply(clientId = Some(clientId))
	
	/**
	  * @param clientSecret This application's password to the referenced service
	  * @return A model containing only the specified clientSecret
	  */
	def withClientSecret(clientSecret: String) = apply(clientSecret = Some(clientSecret))
	
	/**
	  * @param created Time when this AuthServiceSettings was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param defaultCompletionRedirectUrl Url on the client side (front) where the user will be redirected upon authentication completion. Used if no redirect urls were prepared by the client.
	  * @return A model containing only the specified defaultCompletionRedirectUrl
	  */
	def withDefaultCompletionRedirectUrl(defaultCompletionRedirectUrl: String) = 
		apply(defaultCompletionRedirectUrl = Some(defaultCompletionRedirectUrl))
	
	/**
	  * @param defaultSessionDuration Duration of this AuthServiceSettings
	  * @return A model containing only the specified defaultSessionDuration
	  */
	def withdefaultSessionDuration(defaultSessionDuration: FiniteDuration) =
		apply(defaultSessionDuration = Some(defaultSessionDuration))
	
	/**
	  * @param id A AuthServiceSettings id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param incompleteAuthRedirectUrl Url on the client side (front) that receives the user when they arrive from an OAuth process that was not initiated in this application. None if this use case is not supported.
	  * @return A model containing only the specified incompleteAuthRedirectUrl
	  */
	def withIncompleteAuthRedirectUrl(incompleteAuthRedirectUrl: String) = 
		apply(incompleteAuthRedirectUrl = Some(incompleteAuthRedirectUrl))
	
	/**
	  * @param incompleteAuthTokenDuration Duration how long incomplete authentication tokens can be used after they're issued before they expire
	  * @return A model containing only the specified incompleteAuthTokenDuration
	  */
	def withIncompleteAuthTokenDuration(incompleteAuthTokenDuration: FiniteDuration) = 
		apply(incompleteAuthTokenDuration = Some(incompleteAuthTokenDuration))
	
	/**
	  * @param preparationTokenDuration Duration how long preparation tokens can be used after they're issued before they expire
	  * @return A model containing only the specified preparationTokenDuration
	  */
	def withPreparationTokenDuration(preparationTokenDuration: FiniteDuration) = 
		apply(preparationTokenDuration = Some(preparationTokenDuration))
	
	/**
	  * @param redirectTokenDuration Duration how long redirect tokens can be used after they're issued before they expire
	  * @return A model containing only the specified redirectTokenDuration
	  */
	def withRedirectTokenDuration(redirectTokenDuration: FiniteDuration) = 
		apply(redirectTokenDuration = Some(redirectTokenDuration))
	
	/**
	  * @param redirectUrl Url to the endpoint in this application which receives the user after they've completed the OAuth process
	  * @return A model containing only the specified redirectUrl
	  */
	def withRedirectUrl(redirectUrl: String) = apply(redirectUrl = Some(redirectUrl))
	
	/**
	  * @param serviceId Id of the described service
	  * @return A model containing only the specified serviceId
	  */
	def withServiceId(serviceId: Int) = apply(serviceId = Some(serviceId))
	
	/**
	  * @param tokenUrl Url to the endpoint that provides refresh and session tokens
	  * @return A model containing only the specified tokenUrl
	  */
	def withTokenUrl(tokenUrl: String) = apply(tokenUrl = Some(tokenUrl))
}

/**
  * Used for interacting with AuthServiceSettings in the database
  * @param id AuthServiceSettings database id
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
  * @param defaultSessionDuration Duration of this AuthServiceSettings
  * @param created Time when this AuthServiceSettings was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthServiceSettingsModel(id: Option[Int] = None, serviceId: Option[Int] = None, 
	clientId: Option[String] = None, clientSecret: Option[String] = None, 
	authenticationUrl: Option[String] = None, tokenUrl: Option[String] = None, 
	redirectUrl: Option[String] = None, incompleteAuthRedirectUrl: Option[String] = None, 
	defaultCompletionRedirectUrl: Option[String] = None, 
	preparationTokenDuration: Option[FiniteDuration] = None, 
	redirectTokenDuration: Option[FiniteDuration] = None, 
	incompleteAuthTokenDuration: Option[FiniteDuration] = None, 
	defaultSessionDuration: Option[FiniteDuration] = None, created: Option[Instant] = None)
	extends StorableWithFactory[AuthServiceSettings]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceSettingsModel.factory
	
	override def valueProperties = 
	{
		import AuthServiceSettingsModel._
		Vector("id" -> id, serviceIdAttName -> serviceId, clientIdAttName -> clientId, 
			clientSecretAttName -> clientSecret, authenticationUrlAttName -> authenticationUrl, 
			tokenUrlAttName -> tokenUrl, redirectUrlAttName -> redirectUrl, 
			incompleteAuthRedirectUrlAttName -> incompleteAuthRedirectUrl, 
			defaultCompletionRedirectUrlAttName -> defaultCompletionRedirectUrl, 
			preparationTokenDurationAttName -> preparationTokenDuration.map { _.toUnit(TimeUnit.MINUTES) }, 
			redirectTokenDurationAttName -> redirectTokenDuration.map { _.toUnit(TimeUnit.MINUTES) }, 
			incompleteAuthTokenDurationAttName -> incompleteAuthTokenDuration.map { _.toUnit(TimeUnit.MINUTES) }, 
			defaultSessionDurationAttName -> defaultSessionDuration.map { _.toUnit(TimeUnit.MINUTES) },
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param authenticationUrl A new authenticationUrl
	  * @return A new copy of this model with the specified authenticationUrl
	  */
	def withAuthenticationUrl(authenticationUrl: String) = copy(authenticationUrl = Some(authenticationUrl))
	
	/**
	  * @param clientId A new clientId
	  * @return A new copy of this model with the specified clientId
	  */
	def withClientId(clientId: String) = copy(clientId = Some(clientId))
	
	/**
	  * @param clientSecret A new clientSecret
	  * @return A new copy of this model with the specified clientSecret
	  */
	def withClientSecret(clientSecret: String) = copy(clientSecret = Some(clientSecret))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param defaultCompletionRedirectUrl A new defaultCompletionRedirectUrl
	  * @return A new copy of this model with the specified defaultCompletionRedirectUrl
	  */
	def withDefaultCompletionRedirectUrl(defaultCompletionRedirectUrl: String) = 
		copy(defaultCompletionRedirectUrl = Some(defaultCompletionRedirectUrl))
	
	/**
	  * @param defaultSessionDuration A new defaultSessionDuration
	  * @return A new copy of this model with the specified defaultSessionDuration
	  */
	def withDefaultSessionDuration(defaultSessionDuration: FiniteDuration) =
		copy(defaultSessionDuration = Some(defaultSessionDuration))
	
	/**
	  * @param incompleteAuthRedirectUrl A new incompleteAuthRedirectUrl
	  * @return A new copy of this model with the specified incompleteAuthRedirectUrl
	  */
	def withIncompleteAuthRedirectUrl(incompleteAuthRedirectUrl: String) = 
		copy(incompleteAuthRedirectUrl = Some(incompleteAuthRedirectUrl))
	
	/**
	  * @param incompleteAuthTokenDuration A new incompleteAuthTokenDuration
	  * @return A new copy of this model with the specified incompleteAuthTokenDuration
	  */
	def withIncompleteAuthTokenDuration(incompleteAuthTokenDuration: FiniteDuration) = 
		copy(incompleteAuthTokenDuration = Some(incompleteAuthTokenDuration))
	
	/**
	  * @param preparationTokenDuration A new preparationTokenDuration
	  * @return A new copy of this model with the specified preparationTokenDuration
	  */
	def withPreparationTokenDuration(preparationTokenDuration: FiniteDuration) = 
		copy(preparationTokenDuration = Some(preparationTokenDuration))
	
	/**
	  * @param redirectTokenDuration A new redirectTokenDuration
	  * @return A new copy of this model with the specified redirectTokenDuration
	  */
	def withRedirectTokenDuration(redirectTokenDuration: FiniteDuration) = 
		copy(redirectTokenDuration = Some(redirectTokenDuration))
	
	/**
	  * @param redirectUrl A new redirectUrl
	  * @return A new copy of this model with the specified redirectUrl
	  */
	def withRedirectUrl(redirectUrl: String) = copy(redirectUrl = Some(redirectUrl))
	
	/**
	  * @param serviceId A new serviceId
	  * @return A new copy of this model with the specified serviceId
	  */
	def withServiceId(serviceId: Int) = copy(serviceId = Some(serviceId))
	
	/**
	  * @param tokenUrl A new tokenUrl
	  * @return A new copy of this model with the specified tokenUrl
	  */
	def withTokenUrl(tokenUrl: String) = copy(tokenUrl = Some(tokenUrl))
}

