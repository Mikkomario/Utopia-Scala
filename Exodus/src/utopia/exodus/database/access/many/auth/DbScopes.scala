package utopia.exodus.database.access.many.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbScopes extends ManyScopesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted scopes
	  * @return An access point to scopes with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbScopesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbScopesSubset(targetIds: Set[Int]) extends ManyScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

