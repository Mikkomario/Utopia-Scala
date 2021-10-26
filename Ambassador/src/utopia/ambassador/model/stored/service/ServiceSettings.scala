package utopia.ambassador.model.stored.service

import utopia.ambassador.model.partial.service.ServiceSettingsData
import utopia.vault.model.template.Stored

/**
  * Represents a set of 3rd party service-specific settings that have been stored to the DB
  * @author Mikko Hilpinen
  * @since 14.7.2021, v1.0
  */
@deprecated("Replaced with AuthServiceSettings", "v2.0")
case class ServiceSettings(id: Int, data: ServiceSettingsData) extends Stored[ServiceSettingsData, Int]
