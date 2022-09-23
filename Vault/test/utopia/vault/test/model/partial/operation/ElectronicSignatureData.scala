package utopia.vault.test.model.partial.operation

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

import java.time.Instant

/**
  * Represents a received e-signature
  * @param issuerName Name of the certificate issuer, as specified in the signature
  * @param serialNumber Serial number of the certificate, as specified in the signature
  * @param created Time when this electronic signature was added to the database
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
case class ElectronicSignatureData(issuerName: String = "", serialNumber: String = "", 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("issuer_name" -> issuerName, "serial_number" -> serialNumber, "created" -> created))
}

