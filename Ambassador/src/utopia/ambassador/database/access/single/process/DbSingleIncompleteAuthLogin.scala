package utopia.ambassador.database.access.single.process

import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual IncompleteAuthLogins, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleIncompleteAuthLogin(id: Int) 
	extends UniqueIncompleteAuthLoginAccess with SingleIntIdModelAccess[IncompleteAuthLogin]

