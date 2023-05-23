package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.api.database.model.logging.IssueVariantModel
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual issue variants
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssueVariant extends SingleRowModelAccess[IssueVariant] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueVariantModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted issue variant
	  * @return An access point to that issue variant
	  */
	def apply(id: Int) = DbSingleIssueVariant(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique issue variants.
	  * @return An access point to the issue variant that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueIssueVariantAccess(mergeCondition(condition))
}

