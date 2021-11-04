package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.DeviceToken
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual DeviceTokens, based on their id
  * @since 2021-10-25
  */
case class DbSingleDeviceToken(id: Int) 
	extends UniqueDeviceTokenAccess with SingleIntIdModelAccess[DeviceToken]

