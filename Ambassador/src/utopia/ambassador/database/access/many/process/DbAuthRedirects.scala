package utopia.ambassador.database.access.many.process

import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple AuthRedirects at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthRedirects extends ManyAuthRedirectsAccess with NonDeprecatedView[AuthRedirect]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthRedirects
	  * @return An access point to AuthRedirects with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthRedirectsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthRedirectsSubset(targetIds: Set[Int]) extends ManyAuthRedirectsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

