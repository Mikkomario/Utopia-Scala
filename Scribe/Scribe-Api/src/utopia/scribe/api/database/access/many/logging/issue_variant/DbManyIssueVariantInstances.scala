package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple issue variant instances at a time
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
object DbManyIssueVariantInstances extends ManyIssueVariantInstancesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted issue variant instances
	  * @return An access point to issue variant instances with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbIssueVariantInstancesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbIssueVariantInstancesSubset(targetIds: Set[Int]) extends ManyIssueVariantInstancesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

