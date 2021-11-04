package utopia.citadel.database.access.single.device

import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual ClientDeviceUsers, based on their id
  * @since 2021-10-23
  */
case class DbSingleClientDeviceUser(id: Int) 
	extends UniqueClientDeviceUserAccess with SingleIntIdModelAccess[ClientDeviceUser]

