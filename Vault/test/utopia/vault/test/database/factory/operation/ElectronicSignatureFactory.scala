package utopia.vault.test.database.factory.operation

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.test.database.VaultTestTables
import utopia.vault.test.model.partial.operation.ElectronicSignatureData
import utopia.vault.test.model.stored.operation.ElectronicSignature

/**
  * Used for reading electronic signature data from the DB
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
object ElectronicSignatureFactory 
	extends FromValidatedRowModelFactory[ElectronicSignature] 
		with FromRowFactoryWithTimestamps[ElectronicSignature]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = VaultTestTables.electronicSignature
	
	override def fromValidatedModel(valid: Model) = 
		ElectronicSignature(valid("id").getInt, ElectronicSignatureData(valid("issuerName").getString, 
			valid("serialNumber").getString, valid("created").getInstant))
}

