package utopia.ambassador.database.access.single.process

import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthRedirects, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthRedirect(id: Int) 
	extends UniqueAuthRedirectAccess with SingleIntIdModelAccess[AuthRedirect]
{
	/**
	  * @return An access point to this redirect's result
	  */
	def result = DbAuthRedirectResult.forRedirectWithId(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether this redirection already has a response
	  */
	def isCompleted(implicit connection: Connection) = result.nonEmpty
}