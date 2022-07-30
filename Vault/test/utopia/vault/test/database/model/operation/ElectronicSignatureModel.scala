package utopia.vault.test.database.model.operation

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.test.database.factory.operation.ElectronicSignatureFactory
import utopia.vault.test.model.partial.operation.ElectronicSignatureData
import utopia.vault.test.model.stored.operation.ElectronicSignature

import java.time.Instant

/**
  * Used for constructing ElectronicSignatureModel instances and for inserting electronic signatures to
  *  the database
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
object ElectronicSignatureModel 
	extends DataInserter[ElectronicSignatureModel, ElectronicSignature, ElectronicSignatureData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains electronic signature issuer name
	  */
	val issuerNameAttName = "issuerName"
	
	/**
	  * Name of the property that contains electronic signature serial number
	  */
	val serialNumberAttName = "serialNumber"
	
	/**
	  * Name of the property that contains electronic signature created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains electronic signature issuer name
	  */
	def issuerNameColumn = table(issuerNameAttName)
	
	/**
	  * Column that contains electronic signature serial number
	  */
	def serialNumberColumn = table(serialNumberAttName)
	
	/**
	  * Column that contains electronic signature created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ElectronicSignatureFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ElectronicSignatureData) = 
		apply(None, data.issuerName, data.serialNumber, Some(data.created))
	
	override def complete(id: Value, data: ElectronicSignatureData) = ElectronicSignature(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this electronic signature was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A electronic signature id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param issuerName Name of the certificate issuer, as specified in the signature
	  * @return A model containing only the specified issuer name
	  */
	def withIssuerName(issuerName: String) = apply(issuerName = issuerName)
	
	/**
	  * @param serialNumber Serial number of the certificate, as specified in the signature
	  * @return A model containing only the specified serial number
	  */
	def withSerialNumber(serialNumber: String) = apply(serialNumber = serialNumber)
}

/**
  * Used for interacting with ElectronicSignatures in the database
  * @param id electronic signature database id
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
case class ElectronicSignatureModel(id: Option[Int] = None, issuerName: String = "", 
	serialNumber: String = "", created: Option[Instant] = None) 
	extends StorableWithFactory[ElectronicSignature]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ElectronicSignatureModel.factory
	
	override def valueProperties = {
		import ElectronicSignatureModel._
		Vector("id" -> id, issuerNameAttName -> issuerName, serialNumberAttName -> serialNumber, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this electronic signature was added to the database
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param issuerName Name of the certificate issuer, as specified in the signature
	  * @return A new copy of this model with the specified issuer name
	  */
	def withIssuerName(issuerName: String) = copy(issuerName = issuerName)
	
	/**
	  * @param serialNumber Serial number of the certificate, as specified in the signature
	  * @return A new copy of this model with the specified serial number
	  */
	def withSerialNumber(serialNumber: String) = copy(serialNumber = serialNumber)
}

