package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.api.database.factory.logging.ContextualIssueVariantFactory
import utopia.scribe.api.database.model.logging.{IssueModel, IssueVariantModel}
import utopia.scribe.core.model.combined.logging.ContextualIssueVariant
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual contextual issue variants
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
object DbContextualIssueVariant 
	extends SingleRowModelAccess[ContextualIssueVariant] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with linked variants
	  */
	protected def model = IssueVariantModel
	
	/**
	  * A database model (factory) used for interacting with the linked issue
	  */
	protected def issueModel = IssueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ContextualIssueVariantFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted contextual issue variant
	  * @return An access point to that contextual issue variant
	  */
	def apply(id: Int) = DbSingleContextualIssueVariant(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique contextual issue variants.
	  * @return An access point to the contextual issue variant that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = 
		UniqueContextualIssueVariantAccess(mergeCondition(condition))
}

