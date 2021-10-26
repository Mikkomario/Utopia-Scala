package utopia.ambassador.database.access.single.service

import utopia.ambassador.model.stored.service.AuthService
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthServices, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthService(id: Int) 
	extends UniqueAuthServiceAccess with SingleIntIdModelAccess[AuthService]

