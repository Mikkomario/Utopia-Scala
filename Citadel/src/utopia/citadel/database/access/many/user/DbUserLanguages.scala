package utopia.citadel.database.access.many.user

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple UserLanguages at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserLanguages extends ManyUserLanguagesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted UserLanguages
	  * @return An access point to UserLanguages with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUserLanguagesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUserLanguagesSubset(targetIds: Set[Int]) extends ManyUserLanguagesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

