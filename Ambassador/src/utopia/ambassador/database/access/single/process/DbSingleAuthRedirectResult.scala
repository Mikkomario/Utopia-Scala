package utopia.ambassador.database.access.single.process

import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthRedirectResults, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthRedirectResult(id: Int) 
	extends UniqueAuthRedirectResultAccess with SingleIntIdModelAccess[AuthRedirectResult]

