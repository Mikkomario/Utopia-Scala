package utopia.exodus.database.access.many.auth

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple EmailValidationPurposes at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbEmailValidationPurposes extends ManyEmailValidationPurposesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted EmailValidationPurposes
	  * @return An access point to EmailValidationPurposes with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidationPurposesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationPurposesSubset(targetIds: Set[Int]) extends ManyEmailValidationPurposesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

