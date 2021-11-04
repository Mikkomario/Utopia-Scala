package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleAuthPreparation
import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.metropolis.model.stored.StyledStoredModelConvertible

/**
  * Represents a AuthPreparation that has already been stored in the database
  * @param id id of this AuthPreparation in the database
  * @param data Wrapped AuthPreparation data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparation(id: Int, data: AuthPreparationData)
	extends StyledStoredModelConvertible[AuthPreparationData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthPreparation in the database
	  */
	def access = DbSingleAuthPreparation(id)
	
	
	// IMPLEMENTED  ----------------
	
	override protected def includeIdInSimpleModel = false
}

