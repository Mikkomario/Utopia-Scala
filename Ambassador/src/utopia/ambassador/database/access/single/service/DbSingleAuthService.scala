package utopia.ambassador.database.access.single.service

import utopia.ambassador.database.access.many.scope.DbScopes
import utopia.ambassador.model.stored.service.AuthService
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthServices, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthService(id: Int) 
	extends UniqueAuthServiceAccess with SingleIntIdModelAccess[AuthService]
{
	/**
	  * @return An access point to settings concerning this authentication service
	  */
	def settings = DbAuthServiceSettings.forServiceWithId(id)
	
	/**
	  * @return An access point to scopes for this service
	  */
	def scopes = DbScopes.forServiceWithId(id)
}