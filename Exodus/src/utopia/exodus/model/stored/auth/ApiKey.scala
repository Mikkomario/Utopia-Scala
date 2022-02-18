package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleApiKey
import utopia.exodus.model.partial.auth.ApiKeyData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a ApiKey that has already been stored in the database
  * @param id id of this ApiKey in the database
  * @param data Wrapped ApiKey data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class ApiKey(id: Int, data: ApiKeyData) extends StoredModelConvertible[ApiKeyData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this ApiKey in the database
	  */
	def access = DbSingleApiKey(id)
}

