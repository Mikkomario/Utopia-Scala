package utopia.vault.test.database.access.single.operation

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.vault.test.model.stored.operation.ElectronicSignature

/**
  * An access point to individual electronic signatures, based on their id
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
case class DbSingleElectronicSignature(id: Int) 
	extends UniqueElectronicSignatureAccess with SingleIntIdModelAccess[ElectronicSignature]

