package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple issue variants at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssueVariants extends ManyIssueVariantsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted issue variants
	  * @return An access point to issue variants with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbIssueVariantsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbIssueVariantsSubset(targetIds: Set[Int]) extends ManyIssueVariantsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

