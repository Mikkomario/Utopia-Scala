package utopia.ambassador.database.access.many.service

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import utopia.ambassador.database.factory.service.AuthServiceSettingsFactory
import utopia.ambassador.database.model.service.AuthServiceSettingsModel
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

import java.util.concurrent.TimeUnit

object ManyAuthServiceSettingsAccess
{
	// NESTED	--------------------
	
	private class ManyAuthServiceSettingsSubView(override val parent: ManyRowModelAccess[AuthServiceSettings], 
		override val filterCondition: Condition) 
		extends ManyAuthServiceSettingsAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthServiceSettings at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthServiceSettingsAccess
	extends ManyRowModelAccess[AuthServiceSettings] with Indexed with FilterableView[ManyAuthServiceSettingsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * serviceIds of the accessible AuthServiceSettings
	  */
	def serviceIds(implicit connection: Connection) = 
		pullColumn(model.serviceIdColumn).flatMap { value => value.int }
	
	/**
	  * clientIds of the accessible AuthServiceSettings
	  */
	def clientIds(implicit connection: Connection) = 
		pullColumn(model.clientIdColumn).flatMap { value => value.string }
	
	/**
	  * clientSecrets of the accessible AuthServiceSettings
	  */
	def clientSecrets(implicit connection: Connection) = 
		pullColumn(model.clientSecretColumn).flatMap { value => value.string }
	
	/**
	  * authenticationUrls of the accessible AuthServiceSettings
	  */
	def authenticationUrls(implicit connection: Connection) = 
		pullColumn(model.authenticationUrlColumn).flatMap { value => value.string }
	
	/**
	  * tokenUrls of the accessible AuthServiceSettings
	  */
	def tokenUrls(implicit connection: Connection) = 
		pullColumn(model.tokenUrlColumn).flatMap { value => value.string }
	
	/**
	  * redirectUrls of the accessible AuthServiceSettings
	  */
	def redirectUrls(implicit connection: Connection) = 
		pullColumn(model.redirectUrlColumn).flatMap { value => value.string }
	
	/**
	  * incompleteAuthRedirectUrls of the accessible AuthServiceSettings
	  */
	def incompleteAuthRedirectUrls(implicit connection: Connection) = 
		pullColumn(model.incompleteAuthRedirectUrlColumn).flatMap { value => value.string }
	
	/**
	  * defaultCompletionRedirectUrls of the accessible AuthServiceSettings
	  */
	def defaultCompletionRedirectUrls(implicit connection: Connection) = 
		pullColumn(model.defaultCompletionRedirectUrlColumn).flatMap { value => value.string }
	
	/**
	  * preparationTokenDurations of the accessible AuthServiceSettings
	  */
	def preparationTokenDurations(implicit connection: Connection) = 
		pullColumn(model.preparationTokenDurationColumn).flatMap { value => value.long.map { FiniteDuration(_, 
			TimeUnit.MINUTES) } }
	
	/**
	  * redirectTokenDurations of the accessible AuthServiceSettings
	  */
	def redirectTokenDurations(implicit connection: Connection) = 
		pullColumn(model.redirectTokenDurationColumn).flatMap { value => value.long.map { FiniteDuration(_, 
			TimeUnit.MINUTES) } }
	
	/**
	  * incompleteAuthTokenDurations of the accessible AuthServiceSettings
	  */
	def incompleteAuthTokenDurations(implicit connection: Connection) = 
		pullColumn(model.incompleteAuthTokenDurationColumn).flatMap { value => value.long.map { FiniteDuration(_, 
			TimeUnit.MINUTES) } }
	
	/**
	  * DefaultSessionDurations of the accessible AuthServiceSettings
	  */
	def defaultSessionDurations(implicit connection: Connection) =
		pullColumn(model.defaultSessionDurationColumn).flatMap { value => value.long.map { FiniteDuration(_,
			TimeUnit.MINUTES) } }
	
	/**
	  * creationTimes of the accessible AuthServiceSettings
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthServiceSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceSettingsFactory
	
	override def filter(additionalCondition: Condition): ManyAuthServiceSettingsAccess = 
		new ManyAuthServiceSettingsAccess.ManyAuthServiceSettingsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the DefaultSessionDuration of the targeted AuthServiceSettings instance(s)
	  * @param newDefaultSessionDuration A new DefaultSessionDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def defaultSessionDurations_=(newDefaultSessionDuration: FiniteDuration)(implicit connection: Connection) =
		putColumn(model.defaultSessionDurationColumn, newDefaultSessionDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the authenticationUrl of the targeted AuthServiceSettings instance(s)
	  * @param newAuthenticationUrl A new authenticationUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def authenticationUrls_=(newAuthenticationUrl: String)(implicit connection: Connection) = 
		putColumn(model.authenticationUrlColumn, newAuthenticationUrl)
	
	/**
	  * Updates the clientId of the targeted AuthServiceSettings instance(s)
	  * @param newClientId A new clientId to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def clientIds_=(newClientId: String)(implicit connection: Connection) = 
		putColumn(model.clientIdColumn, newClientId)
	
	/**
	  * Updates the clientSecret of the targeted AuthServiceSettings instance(s)
	  * @param newClientSecret A new clientSecret to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def clientSecrets_=(newClientSecret: String)(implicit connection: Connection) = 
		putColumn(model.clientSecretColumn, newClientSecret)
	
	/**
	  * Updates the created of the targeted AuthServiceSettings instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the defaultCompletionRedirectUrl of the targeted AuthServiceSettings instance(s)
	  * @param newDefaultCompletionRedirectUrl A new defaultCompletionRedirectUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def defaultCompletionRedirectUrls_=(newDefaultCompletionRedirectUrl: String)(implicit connection: Connection) = 
		putColumn(model.defaultCompletionRedirectUrlColumn, newDefaultCompletionRedirectUrl)
	
	/**
	  * Updates the incompleteAuthRedirectUrl of the targeted AuthServiceSettings instance(s)
	  * @param newIncompleteAuthRedirectUrl A new incompleteAuthRedirectUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def incompleteAuthRedirectUrls_=(newIncompleteAuthRedirectUrl: String)(implicit connection: Connection) = 
		putColumn(model.incompleteAuthRedirectUrlColumn, newIncompleteAuthRedirectUrl)
	
	/**
	  * Updates the incompleteAuthTokenDuration of the targeted AuthServiceSettings instance(s)
	  * @param newIncompleteAuthTokenDuration A new incompleteAuthTokenDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def incompleteAuthTokenDurations_=(newIncompleteAuthTokenDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.incompleteAuthTokenDurationColumn, 
			newIncompleteAuthTokenDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the preparationTokenDuration of the targeted AuthServiceSettings instance(s)
	  * @param newPreparationTokenDuration A new preparationTokenDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def preparationTokenDurations_=(newPreparationTokenDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.preparationTokenDurationColumn, newPreparationTokenDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the redirectTokenDuration of the targeted AuthServiceSettings instance(s)
	  * @param newRedirectTokenDuration A new redirectTokenDuration to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def redirectTokenDurations_=(newRedirectTokenDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.redirectTokenDurationColumn, newRedirectTokenDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the redirectUrl of the targeted AuthServiceSettings instance(s)
	  * @param newRedirectUrl A new redirectUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def redirectUrls_=(newRedirectUrl: String)(implicit connection: Connection) = 
		putColumn(model.redirectUrlColumn, newRedirectUrl)
	
	/**
	  * Updates the serviceId of the targeted AuthServiceSettings instance(s)
	  * @param newServiceId A new serviceId to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def serviceIds_=(newServiceId: Int)(implicit connection: Connection) = 
		putColumn(model.serviceIdColumn, newServiceId)
	
	/**
	  * Updates the tokenUrl of the targeted AuthServiceSettings instance(s)
	  * @param newTokenUrl A new tokenUrl to assign
	  * @return Whether any AuthServiceSettings instance was affected
	  */
	def tokenUrls_=(newTokenUrl: String)(implicit connection: Connection) = 
		putColumn(model.tokenUrlColumn, newTokenUrl)
}

