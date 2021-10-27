package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.access.many.process.{DbAuthCompletionRedirectTargets, DbAuthPreparationScopeLinks}
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthPreparations, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthPreparation(id: Int) 
	extends UniqueAuthPreparationAccess with SingleIntIdModelAccess[AuthPreparation]
{
	/**
	  * @return An access point to the user redirect event prepared by this preparation
	  */
	def redirect = DbAuthRedirect.forPreparationWithId(id)
	/**
	  * @return An access point to redirection targets specified during this authentication preparation
	  */
	def redirectTargets =
		DbAuthCompletionRedirectTargets.forPreparationWithId(id)
	/**
	  * @return An access point to links to this preparation's requested scopes
	  */
	def scopeLinks = DbAuthPreparationScopeLinks.withPreparationId(id)
	/**
	  * @return An access point to scopes requested during this preparation
	  */
	def scopes = scopeLinks.withScopes
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether this authentication preparation has been consumed (redirected) already
	  */
	def isConsumed(implicit connection: Connection) = redirect.nonEmpty
}
