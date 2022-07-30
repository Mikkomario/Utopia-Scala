package utopia.vault.test.database.access.many.operation

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple electronic signatures at a time
  * @author Mikko Hilpinen
  * @since 30.07.2022, v1.13
  */
object DbElectronicSignatures extends ManyElectronicSignaturesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted electronic signatures
	  * @return An access point to electronic signatures with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbElectronicSignaturesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbElectronicSignaturesSubset(targetIds: Set[Int]) extends ManyElectronicSignaturesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

