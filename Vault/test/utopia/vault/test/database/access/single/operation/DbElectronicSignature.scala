package utopia.vault.test.database.access.single.operation

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.test.database.factory.operation.ElectronicSignatureFactory
import utopia.vault.test.database.model.operation.ElectronicSignatureModel
import utopia.vault.test.model.stored.operation.ElectronicSignature

/**
  * Used for accessing individual electronic signatures
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
object DbElectronicSignature 
	extends SingleRowModelAccess[ElectronicSignature] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ElectronicSignatureModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ElectronicSignatureFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted electronic signature
	  * @return An access point to that electronic signature
	  */
	def apply(id: Int) = DbSingleElectronicSignature(id)
}

