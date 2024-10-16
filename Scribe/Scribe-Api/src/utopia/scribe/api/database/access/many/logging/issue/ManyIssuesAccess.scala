package utopia.scribe.api.database.access.many.logging.issue

import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

object ManyIssuesAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyIssuesAccess = new ManyIssuesSubView(condition)
	
	
	// NESTED	--------------------
	
	private class ManyIssuesSubView(condition: Condition) extends ManyIssuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
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
	// COMPUTED	--------------------
	
	/**
	  * Copy of this access that includes variant and occurrence data
	  */
	def instances = DbManyIssueInstances.filter(accessCondition)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyIssuesAccess = ManyIssuesAccess(condition)
}

