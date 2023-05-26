package utopia.scribe.api.database.access.many.logging.issue

import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

object ManyIssuesAccess
{
	// NESTED	--------------------
	
	private class ManyIssuesSubView(condition: Condition) extends ManyIssuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple issues at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyIssuesAccess 
	extends ManyIssuesAccessLike[Issue, ManyIssuesAccess] with ManyRowModelAccess[Issue] 
		with ChronoRowFactoryView[Issue, ManyIssuesAccess]
{
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this access that includes variant and occurrence data
	  */
	def instances = DbManyIssueInstances.filter(globalCondition)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyIssuesAccess = 
		new ManyIssuesAccess.ManyIssuesSubView(mergeCondition(filterCondition))
}

