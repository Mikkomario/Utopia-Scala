package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleIncompleteAuth
import utopia.ambassador.model.partial.process.IncompleteAuthData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a IncompleteAuth that has already been stored in the database
  * @param id id of this IncompleteAuth in the database
  * @param data Wrapped IncompleteAuth data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class IncompleteAuth(id: Int, data: IncompleteAuthData) 
	extends StoredModelConvertible[IncompleteAuthData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this IncompleteAuth in the database
	  */
	def access = DbSingleIncompleteAuth(id)
}

