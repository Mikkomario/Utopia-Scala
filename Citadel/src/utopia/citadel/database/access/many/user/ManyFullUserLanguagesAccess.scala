package utopia.citadel.database.access.many.user

import utopia.citadel.database.factory.user.FullUserLanguageFactory
import utopia.metropolis.model.combined.user.FullUserLanguage
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyFullUserLanguagesAccess extends ViewFactory[ManyFullUserLanguagesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyFullUserLanguagesAccess = 
		new _ManyFullUserLanguagesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyFullUserLanguagesAccess(condition: Condition) extends ManyFullUserLanguagesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * Common trait for access points which return multiple full user language sets at once
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyFullUserLanguagesAccess 
	extends ManyUserLanguageLinksAccessLike[FullUserLanguage, ManyFullUserLanguagesAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = FullUserLanguageFactory
	
	override protected def self = this
	
	
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyFullUserLanguagesAccess = ManyFullUserLanguagesAccess(condition)
}

