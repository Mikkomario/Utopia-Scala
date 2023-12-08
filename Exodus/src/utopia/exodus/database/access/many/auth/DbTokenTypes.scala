package utopia.exodus.database.access.many.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple token types at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbTokenTypes extends ManyTokenTypesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted token types
	  * @return An access point to token types with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbTokenTypesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbTokenTypesSubset(targetIds: Set[Int]) extends ManyTokenTypesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

