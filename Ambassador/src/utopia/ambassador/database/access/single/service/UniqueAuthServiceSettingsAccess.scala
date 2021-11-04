package utopia.ambassador.database.access.single.service

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.ambassador.database.factory.service.AuthServiceSettingsFactory
import utopia.ambassador.database.model.service.AuthServiceSettingsModel
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthServiceSettings.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthServiceSettingsAccess 
	extends SingleRowModelAccess[AuthServiceSettings] 
		with DistinctModelAccess[AuthServiceSettings, Option[AuthServiceSettings], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the described service. None if no instance (or value) was found.
	  */
	def serviceId(implicit connection: Connection) = pullColumn(model.serviceIdColumn).int
	
	/**
	  * Id of this client in the referenced service. None if no instance (or value) was found.
	  */
	def clientId(implicit connection: Connection) = pullColumn(model.clientIdColumn).string
	
	/**
	  * This application's password to the referenced service. None if no instance (or value) was found.
	  */
	def clientSecret(implicit connection: Connection) = pullColumn(model.clientSecretColumn).string
	
	/**
	  * Url to the endpoint that receives users for the OAuth process. None if no instance (or value) was found.
	  */
	def authenticationUrl(implicit connection: Connection) = pullColumn(model.authenticationUrlColumn).string
	
	/**
	  * Url to the endpoint that provides refresh and session tokens. None if no instance (or value) was found.
	  */
	def tokenUrl(implicit connection: Connection) = pullColumn(model.tokenUrlColumn).string
	
	/**
	  * Url to the endpoint in this application which receives the user after they've completed the OAuth process. None if no instance (or value) was found.
	  */
	def redirectUrl(implicit connection: Connection) = pullColumn(model.redirectUrlColumn).string
	
	/**
	  * Url on the client side (front) that receives the user when they arrive from an OAuth process that was not initiated in this application. None if this use case is not supported.. None if no instance (or value) was found.
	  */
	def incompleteAuthRedirectUrl(implicit connection: Connection) = 
		pullColumn(model.incompleteAuthRedirectUrlColumn).string
	
	/**
	  * Url on the client side (front) where the user will be redirected upon authentication completion. Used if no redirect urls were prepared by the client.. None if no instance (or value) was found.
	  */
	def defaultCompletionRedirectUrl(implicit connection: Connection) = 
		pullColumn(model.defaultCompletionRedirectUrlColumn).string
	
	/**
	  * Duration how long preparation tokens can be used after they're issued before they expire. None if no instance (or value) was found.
	  */
	def preparationTokenDuration(implicit connection: Connection) = 
		pullColumn(model.preparationTokenDurationColumn).long.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * Duration how long redirect tokens can be used after they're issued before they expire. None if no instance (or value) was found.
	  */
	def redirectTokenDuration(implicit connection: Connection) = 
		pullColumn(model.redirectTokenDurationColumn).long.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * Duration how long incomplete authentication tokens can be used after they're issued before they expire. None if no instance (or value) was found.
	  */
	def incompleteAuthTokenDuration(implicit connection: Connection) = 
		pullColumn(model.incompleteAuthTokenDurationColumn).long.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * Duration of this AuthServiceSettings. None if no instance (or value) was found.
	  */
	def defaultSessionDuration(implicit connection: Connection) =
		pullColumn(model.defaultSessionDurationColumn).long.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * Time when this AuthServiceSettings was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthServiceSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceSettingsFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the DefaultSessionDuration of the targeted AuthServiceSettings instance(s)
	  * @param newDefaultSessionDuration A new DefaultSessionDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def defaultSessionDuration_=(newDefaultSessionDuration: FiniteDuration)(implicit connection: Connection) =
		putColumn(model.defaultSessionDurationColumn, newDefaultSessionDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the authenticationUrl of the targeted AuthServiceSettings instance(s)
	  * @param newAuthenticationUrl A new authenticationUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def authenticationUrl_=(newAuthenticationUrl: String)(implicit connection: Connection) = 
		putColumn(model.authenticationUrlColumn, newAuthenticationUrl)
	
	/**
	  * Updates the clientId of the targeted AuthServiceSettings instance(s)
	  * @param newClientId A new clientId to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def clientId_=(newClientId: String)(implicit connection: Connection) = 
		putColumn(model.clientIdColumn, newClientId)
	
	/**
	  * Updates the clientSecret of the targeted AuthServiceSettings instance(s)
	  * @param newClientSecret A new clientSecret to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def clientSecret_=(newClientSecret: String)(implicit connection: Connection) = 
		putColumn(model.clientSecretColumn, newClientSecret)
	
	/**
	  * Updates the created of the targeted AuthServiceSettings instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the defaultCompletionRedirectUrl of the targeted AuthServiceSettings instance(s)
	  * @param newDefaultCompletionRedirectUrl A new defaultCompletionRedirectUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def defaultCompletionRedirectUrl_=(newDefaultCompletionRedirectUrl: String)(implicit connection: Connection) = 
		putColumn(model.defaultCompletionRedirectUrlColumn, newDefaultCompletionRedirectUrl)
	
	/**
	  * Updates the incompleteAuthRedirectUrl of the targeted AuthServiceSettings instance(s)
	  * @param newIncompleteAuthRedirectUrl A new incompleteAuthRedirectUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def incompleteAuthRedirectUrl_=(newIncompleteAuthRedirectUrl: String)(implicit connection: Connection) = 
		putColumn(model.incompleteAuthRedirectUrlColumn, newIncompleteAuthRedirectUrl)
	
	/**
	  * Updates the incompleteAuthTokenDuration of the targeted AuthServiceSettings instance(s)
	  * @param newIncompleteAuthTokenDuration A new incompleteAuthTokenDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def incompleteAuthTokenDuration_=(newIncompleteAuthTokenDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.incompleteAuthTokenDurationColumn, 
			newIncompleteAuthTokenDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the preparationTokenDuration of the targeted AuthServiceSettings instance(s)
	  * @param newPreparationTokenDuration A new preparationTokenDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def preparationTokenDuration_=(newPreparationTokenDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.preparationTokenDurationColumn, newPreparationTokenDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the redirectTokenDuration of the targeted AuthServiceSettings instance(s)
	  * @param newRedirectTokenDuration A new redirectTokenDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def redirectTokenDuration_=(newRedirectTokenDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.redirectTokenDurationColumn, newRedirectTokenDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the redirectUrl of the targeted AuthServiceSettings instance(s)
	  * @param newRedirectUrl A new redirectUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def redirectUrl_=(newRedirectUrl: String)(implicit connection: Connection) = 
		putColumn(model.redirectUrlColumn, newRedirectUrl)
	
	/**
	  * Updates the serviceId of the targeted AuthServiceSettings instance(s)
	  * @param newServiceId A new serviceId to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def serviceId_=(newServiceId: Int)(implicit connection: Connection) = 
		putColumn(model.serviceIdColumn, newServiceId)
	
	/**
	  * Updates the tokenUrl of the targeted AuthServiceSettings instance(s)
	  * @param newTokenUrl A new tokenUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def tokenUrl_=(newTokenUrl: String)(implicit connection: Connection) = 
		putColumn(model.tokenUrlColumn, newTokenUrl)
}

