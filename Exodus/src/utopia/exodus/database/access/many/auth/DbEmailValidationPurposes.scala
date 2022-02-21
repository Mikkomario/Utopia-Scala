package utopia.exodus.database.access.many.auth

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple email validation purposes at a time
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
object DbEmailValidationPurposes extends ManyEmailValidationPurposesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted email validation purposes
	  * @return An access point to email validation purposes with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidationPurposesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationPurposesSubset(targetIds: Set[Int]) extends ManyEmailValidationPurposesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

