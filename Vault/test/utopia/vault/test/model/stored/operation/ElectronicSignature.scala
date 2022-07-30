package utopia.vault.test.model.stored.operation

import utopia.vault.model.template.StoredModelConvertible
import utopia.vault.test.database.access.single.operation.DbSingleElectronicSignature
import utopia.vault.test.model.partial.operation.ElectronicSignatureData

/**
  * Represents a electronic signature that has already been stored in the database
  * @param id id of this electronic signature in the database
  * @param data Wrapped electronic signature data
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
case class ElectronicSignature(id: Int, data: ElectronicSignatureData) 
	extends StoredModelConvertible[ElectronicSignatureData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this electronic signature in the database
	  */
	def access = DbSingleElectronicSignature(id)
}

