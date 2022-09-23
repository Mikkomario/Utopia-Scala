package utopia.vault.test.database.access.single.operation

import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.test.database.factory.operation.ElectronicSignatureFactory
import utopia.vault.test.database.model.operation.ElectronicSignatureModel
import utopia.vault.test.model.stored.operation.ElectronicSignature

import java.time.Instant

/**
  * A common trait for access points that return individual and distinct electronic signatures.
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
trait UniqueElectronicSignatureAccess 
	extends SingleRowModelAccess[ElectronicSignature] 
		with DistinctModelAccess[ElectronicSignature, Option[ElectronicSignature], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Name of the certificate issuer, 
	  * as specified in the signature. None if no electronic signature (or value) was found.
	  */
	def issuerName(implicit connection: Connection) = pullColumn(model.issuerNameColumn).getString
	
	/**
	  * Serial number of the certificate, 
	  * as specified in the signature. None if no electronic signature (or value) was found.
	  */
	def serialNumber(implicit connection: Connection) = pullColumn(model.serialNumberColumn).getString
	
	/**
	  * 
		Time when this electronic signature was added to the database. None if no electronic signature (or value)
	  *  was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ElectronicSignatureModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ElectronicSignatureFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted electronic signatures
	  * @param newCreated A new created to assign
	  * @return Whether any electronic signature was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the issuer names of the targeted electronic signatures
	  * @param newIssuerName A new issuer name to assign
	  * @return Whether any electronic signature was affected
	  */
	def issuerName_=(newIssuerName: String)(implicit connection: Connection) = 
		putColumn(model.issuerNameColumn, newIssuerName)
	
	/**
	  * Updates the serial numbers of the targeted electronic signatures
	  * @param newSerialNumber A new serial number to assign
	  * @return Whether any electronic signature was affected
	  */
	def serialNumber_=(newSerialNumber: String)(implicit connection: Connection) = 
		putColumn(model.serialNumberColumn, newSerialNumber)
}

