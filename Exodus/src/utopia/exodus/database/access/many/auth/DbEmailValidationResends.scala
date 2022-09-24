package utopia.exodus.database.access.many.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple EmailValidationResends at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbEmailValidationResends extends ManyEmailValidationResendsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted EmailValidationResends
	  * @return An access point to EmailValidationResends with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidationResendsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationResendsSubset(targetIds: Set[Int]) extends ManyEmailValidationResendsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

