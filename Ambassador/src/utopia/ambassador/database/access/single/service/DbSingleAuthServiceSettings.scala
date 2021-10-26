package utopia.ambassador.database.access.single.service

import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthServiceSettings, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthServiceSettings(id: Int) 
	extends UniqueAuthServiceSettingsAccess with SingleIntIdModelAccess[AuthServiceSettings]

