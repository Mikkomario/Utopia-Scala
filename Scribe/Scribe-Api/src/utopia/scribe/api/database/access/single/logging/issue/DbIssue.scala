package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual issues
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssue extends SingleRowModelAccess[Issue] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted issue
	  * @return An access point to that issue
	  */
	def apply(id: Int) = DbSingleIssue(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique issues.
	  * @return An access point to the issue that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueIssueAccess(mergeCondition(condition))
}

