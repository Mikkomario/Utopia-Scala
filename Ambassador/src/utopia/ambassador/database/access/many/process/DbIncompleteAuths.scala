package utopia.ambassador.database.access.many.process

import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple IncompleteAuths at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbIncompleteAuths extends ManyIncompleteAuthsAccess with NonDeprecatedView[IncompleteAuth]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted IncompleteAuths
	  * @return An access point to IncompleteAuths with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbIncompleteAuthsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbIncompleteAuthsSubset(targetIds: Set[Int]) extends ManyIncompleteAuthsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

