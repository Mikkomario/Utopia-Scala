package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleIncompleteAuthLogin
import utopia.ambassador.model.partial.process.IncompleteAuthLoginData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a IncompleteAuthLogin that has already been stored in the database
  * @param id id of this IncompleteAuthLogin in the database
  * @param data Wrapped IncompleteAuthLogin data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class IncompleteAuthLogin(id: Int, data: IncompleteAuthLoginData) 
	extends StoredModelConvertible[IncompleteAuthLoginData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this IncompleteAuthLogin in the database
	  */
	def access = DbSingleIncompleteAuthLogin(id)
}

