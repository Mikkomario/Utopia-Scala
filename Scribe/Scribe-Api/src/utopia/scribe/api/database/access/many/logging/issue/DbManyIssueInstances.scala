package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing issues, including their variant and occurrence information
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object DbManyIssueInstances extends ManyIssueInstancesAccess with UnconditionalView
{
	// OTHER    -----------------------
	
	/**
	  * @param ids Targeted issue ids
	  * @return Access to those issues and their data
	  */
	def apply(ids: Set[Int]) = new DbIssueInstancesSubSet(ids)
	
	
	// NESTED   -----------------------
	
	class DbIssueInstancesSubSet(ids: Set[Int]) extends ManyIssueInstancesAccess
	{
		override def accessCondition: Option[Condition] = Some(index in ids)
	}
}
