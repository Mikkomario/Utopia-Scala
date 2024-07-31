package utopia.citadel.database.access.many.user

import utopia.citadel.database.factory.user.UserLanguageLinkWithFamiliarityFactory
import utopia.metropolis.model.combined.user.UserLanguageLinkWithFamiliarity
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyUserLanguageLinksWithFamiliaritiesAccess 
	extends ViewFactory[ManyUserLanguageLinksWithFamiliaritiesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyUserLanguageLinksWithFamiliaritiesAccess = 
		new _ManyUserLanguageLinksWithFamiliaritiesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyUserLanguageLinksWithFamiliaritiesAccess(condition: Condition) 
		extends ManyUserLanguageLinksWithFamiliaritiesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * 
	A common trait for access points which retrieve multiple user language links at a time and include language
  * familiarity information
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyUserLanguageLinksWithFamiliaritiesAccess 
	extends ManyUserLanguageLinksAccessLike[UserLanguageLinkWithFamiliarity, 
		ManyUserLanguageLinksWithFamiliaritiesAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageLinkWithFamiliarityFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyUserLanguageLinksWithFamiliaritiesAccess = 
		ManyUserLanguageLinksWithFamiliaritiesAccess(condition)
}

