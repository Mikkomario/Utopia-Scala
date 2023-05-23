package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple contextual issue variants at a time
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
object DbContextualIssueVariants extends ManyContextualIssueVariantsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted contextual issue variants
	  * @return An access point to contextual issue variants with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbContextualIssueVariantsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbContextualIssueVariantsSubset(targetIds: Set[Int]) extends ManyContextualIssueVariantsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

