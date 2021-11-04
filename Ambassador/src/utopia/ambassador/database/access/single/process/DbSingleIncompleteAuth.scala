package utopia.ambassador.database.access.single.process

import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual IncompleteAuths, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleIncompleteAuth(id: Int) 
	extends UniqueIncompleteAuthAccess with SingleIntIdModelAccess[IncompleteAuth]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return An access point to this authentication attempt's completion login
	  */
	def login = DbIncompleteAuthLogin.forAuthenticationWithId(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether this authentication attempt has been completed with a user login
	  */
	def isCompleted(implicit connection: Connection) = login.nonEmpty
}
