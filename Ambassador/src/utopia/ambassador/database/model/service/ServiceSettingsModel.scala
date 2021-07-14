package utopia.ambassador.database.model.service

import utopia.ambassador.database.factory.service.ServiceSettingsFactory
import utopia.ambassador.model.partial.service.ServiceSettingsData
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object ServiceSettingsModel extends DataInserter[ServiceSettingsModel, ServiceSettings, ServiceSettingsData]
{
	// COMPUTED ---------------------------
	
	/**
	  * @return Factory used by this model type
	  */
	def factory = ServiceSettingsFactory
	
	
	// IMPLEMENTED  -----------------------
	
	override def table = factory.table
	
	override def apply(data: ServiceSettingsData) =
		apply(None, Some(data.serviceId), Some(data.clientId), Some(data.clientSecret),
			Some(data.authenticationUrl), Some(data.tokenUrl), Some(data.redirectUrl),
			data.incompleteAuthUrl, data.defaultCompletionUrl,
			Some(data.preparationTokenDuration), Some(data.redirectTokenDuration),
			Some(data.incompleteAuthTokenDuration), Some(data.defaultSessionDuration), Some(data.created))
	
	override protected def complete(id: Value, data: ServiceSettingsData) = ServiceSettings(id.getInt, data)
}

/**
  * Used for interacting with service-specific settings in the DB
  * @author Mikko Hilpinen
  * @since 14.7.2021, v1.0
  */
case class ServiceSettingsModel(id: Option[Int] = None, serviceId: Option[Int] = None, clientId: Option[String] = None,
                                clientSecret: Option[String] = None, authenticationUrl: Option[String] = None,
                                tokenUrl: Option[String] = None, redirectUrl: Option[String] = None,
                                incompleteAuthUrl: Option[String] = None, defaultCompletionUrl: Option[String] = None,
                                preparationTokenDuration: Option[FiniteDuration] = None,
                                redirectTokenDuration: Option[FiniteDuration] = None,
                                incompleteAuthTokenDuration: Option[FiniteDuration] = None,
                                defaultSessionDuration: Option[FiniteDuration] = None, created: Option[Instant] = None)
	extends StorableWithFactory[ServiceSettings]
{
	override def factory = ServiceSettingsModel.factory
	
	override def valueProperties = Vector("serviceId" -> serviceId, "clientId" -> clientId,
		"clientSecret" -> clientSecret, "authenticationUrl" -> authenticationUrl, "tokenUrl" -> tokenUrl,
		"redirectUrl" -> redirectUrl, "incompleteAuthRedirectUrl" -> incompleteAuthUrl,
		"defaultCompletionRedirectUrl" -> defaultCompletionUrl,
		"preparationTokenExpirationMinutes" -> preparationTokenDuration.map { _.toMinutes },
		"redirectTokenExpirationMinutes" -> redirectTokenDuration.map { _.toMinutes },
		"incompleteAuthTokenExpirationMinutes" -> incompleteAuthTokenDuration.map { _.toMinutes },
		"defaultSessionExpirationMinutes" -> defaultSessionDuration.map { _.toMinutes },
		"created" -> created)
}