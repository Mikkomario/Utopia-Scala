package utopia.logos.database.access.many.url.request_path

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple detailed request paths at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, v0.1
  */
object DbDetailedRequestPaths extends ManyDetailedRequestPathsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted detailed request paths
	  * @return An access point to detailed request paths with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbDetailedRequestPathsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbDetailedRequestPathsSubset(targetIds: Set[Int]) extends ManyDetailedRequestPathsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

