package utopia.scribe.api.database.access.single.logging.issue_occurrence

import utopia.scribe.api.database.factory.logging.IssueOccurrenceFactory
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual issue occurrences
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssueOccurrence extends SingleRowModelAccess[IssueOccurrence] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueOccurrenceFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted issue occurrence
	  * @return An access point to that issue occurrence
	  */
	def apply(id: Int) = DbSingleIssueOccurrence(id)
	
	/**
	  * @param variantIds Ids of the targeted issue variants
	  * @param connection Implicit DB Connection
	  * @return The earliest issue occurrence among any of the targeted variants
	  */
	def earliestAmongVariants(variantIds: Iterable[Int])(implicit connection: Connection) =
		minBy(model.earliestColumn, Some(model.caseIdColumn in variantIds))
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique issue occurrences.
	  * @return An access point to the issue occurrence that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueIssueOccurrenceAccess(mergeCondition(condition))
}

