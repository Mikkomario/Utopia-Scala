package utopia.exodus.database.access.many.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple ApiKeys at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbApiKeys extends ManyApiKeysAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted ApiKeys
	  * @return An access point to ApiKeys with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbApiKeysSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbApiKeysSubset(targetIds: Set[Int]) extends ManyApiKeysAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

