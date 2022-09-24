package utopia.citadel.database.access.many.user

import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple UserSettings at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbManyUserSettings extends ManyUserSettingsAccess with NonDeprecatedView[UserSettings]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted UserSettings
	  * @return An access point to UserSettings with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUserSettingsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUserSettingsSubset(targetIds: Set[Int]) extends ManyUserSettingsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

