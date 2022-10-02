package utopia.ambassador.database.access.many.service

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple AuthServiceSettings at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbManyAuthServiceSettings extends ManyAuthServiceSettingsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthServiceSettings
	  * @return An access point to AuthServiceSettings with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthServiceSettingsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthServiceSettingsSubset(targetIds: Set[Int]) extends ManyAuthServiceSettingsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

