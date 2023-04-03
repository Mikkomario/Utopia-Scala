package utopia.citadel.database.access.many.user

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple UserLanguages at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserLanguageLinks extends ManyUserLanguageLinksAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted UserLanguages
	  * @return An access point to UserLanguages with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUserLanguageLinksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUserLanguageLinksSubset(targetIds: Set[Int]) extends ManyUserLanguageLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

