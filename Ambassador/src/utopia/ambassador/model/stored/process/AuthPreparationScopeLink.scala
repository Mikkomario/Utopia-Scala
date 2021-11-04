package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleAuthPreparationScopeLink
import utopia.ambassador.model.partial.process.AuthPreparationScopeLinkData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthPreparationScopeLink that has already been stored in the database
  * @param id id of this AuthPreparationScopeLink in the database
  * @param data Wrapped AuthPreparationScopeLink data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparationScopeLink(id: Int, data: AuthPreparationScopeLinkData) 
	extends StoredModelConvertible[AuthPreparationScopeLinkData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthPreparationScopeLink in the database
	  */
	def access = DbSingleAuthPreparationScopeLink(id)
}

