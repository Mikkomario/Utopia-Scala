package utopia.scribe.api.database.access.many.logging.issue

import utopia.scribe.api.database.access.many.logging.issue.ManyIssueInstancesAccess.ManyIssueInstancesSubView
import utopia.scribe.api.database.factory.logging.IssueInstancesFactory
import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.Condition

object ManyIssueInstancesAccess
{
	private class ManyIssueInstancesSubView(condition: Condition) extends ManyIssueInstancesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * Common trait for access point that retrieve issues, as well as their variant and occurrence data
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
trait ManyIssueInstancesAccess extends ManyIssuesAccessLike[IssueInstances, ManyIssueInstancesAccess]
{
	// IMPLEMENTED  ------------------------
	
	override protected def self: ManyIssueInstancesAccess = this
	
	override def factory: FromResultFactory[IssueInstances] = IssueInstancesFactory
	
	override def filter(additionalCondition: Condition): ManyIssueInstancesAccess =
		new ManyIssueInstancesSubView(mergeCondition(additionalCondition))
}
