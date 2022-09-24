package utopia.vault.test.database.access.many.operation

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{ChronoRowFactoryView, SubView}
import utopia.vault.sql.Condition
import utopia.vault.test.database.factory.operation.ElectronicSignatureFactory
import utopia.vault.test.database.model.operation.ElectronicSignatureModel
import utopia.vault.test.model.stored.operation.ElectronicSignature

import java.time.Instant

object ManyElectronicSignaturesAccess
{
	// NESTED	--------------------
	
	private class ManyElectronicSignaturesSubView(override val parent: ManyRowModelAccess[ElectronicSignature], 
		override val filterCondition: Condition) 
		extends ManyElectronicSignaturesAccess with SubView
}

/**
  * A common trait for access points which target multiple electronic signatures at a time
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
trait ManyElectronicSignaturesAccess 
	extends ManyRowModelAccess[ElectronicSignature] 
		with ChronoRowFactoryView[ElectronicSignature, ManyElectronicSignaturesAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * issuer names of the accessible electronic signatures
	  */
	def issuerNames(implicit connection: Connection) = pullColumn(model.issuerNameColumn).flatMap { _.string }
	
	/**
	  * serial numbers of the accessible electronic signatures
	  */
	def serialNumbers(implicit connection: Connection) = pullColumn(model.serialNumberColumn)
		.flatMap { _.string }
	
	/**
	  * creation times of the accessible electronic signatures
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ElectronicSignatureModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ElectronicSignatureFactory
	
	override def filter(additionalCondition: Condition): ManyElectronicSignaturesAccess = 
		new ManyElectronicSignaturesAccess.ManyElectronicSignaturesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted electronic signatures
	  * @param newCreated A new created to assign
	  * @return Whether any electronic signature was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the issuer names of the targeted electronic signatures
	  * @param newIssuerName A new issuer name to assign
	  * @return Whether any electronic signature was affected
	  */
	def issuerNames_=(newIssuerName: String)(implicit connection: Connection) = 
		putColumn(model.issuerNameColumn, newIssuerName)
	
	/**
	  * Updates the serial numbers of the targeted electronic signatures
	  * @param newSerialNumber A new serial number to assign
	  * @return Whether any electronic signature was affected
	  */
	def serialNumbers_=(newSerialNumber: String)(implicit connection: Connection) = 
		putColumn(model.serialNumberColumn, newSerialNumber)
}

