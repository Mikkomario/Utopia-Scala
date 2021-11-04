package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleAuthCompletionRedirectTarget
import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthCompletionRedirectTarget that has already been stored in the database
  * @param id id of this AuthCompletionRedirectTarget in the database
  * @param data Wrapped AuthCompletionRedirectTarget data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthCompletionRedirectTarget(id: Int, data: AuthCompletionRedirectTargetData) 
	extends StoredModelConvertible[AuthCompletionRedirectTargetData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthCompletionRedirectTarget in the database
	  */
	def access = DbSingleAuthCompletionRedirectTarget(id)
}

