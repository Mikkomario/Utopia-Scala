package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.api.database.factory.logging.IssueVariantInstancesFactory
import utopia.scribe.api.database.model.logging.{IssueOccurrenceModel, IssueVariantModel}
import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual issue variant instances
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
object DbIssueVariantInstances 
	extends SingleModelAccess[IssueVariantInstances] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with linked variants
	  */
	protected def model = IssueVariantModel
	
	/**
	  * A database model (factory) used for interacting with the linked occurrences
	  */
	protected def occurrenceModel = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantInstancesFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted issue variant instances
	  * @return An access point to that issue variant instances
	  */
	def apply(id: Int) = DbSingleIssueVariantInstances(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique issue variant instances.
	  * @return An access point to the issue variant instances that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = 
		UniqueIssueVariantInstancesAccess(mergeCondition(condition))
}

