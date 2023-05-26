package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.factory.logging.VaryingIssueFactory
import utopia.scribe.api.database.model.logging.{IssueModel, IssueVariantModel}
import utopia.scribe.core.model.combined.logging.VaryingIssue
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual varying issues
  * @author Mikko Hilpinen
  * @since 26.05.2023, v0.1
  */
object DbVaryingIssue extends SingleModelAccess[VaryingIssue] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with linked issues
	  */
	protected def model = IssueModel
	
	/**
	  * A database model (factory) used for interacting with the linked variants
	  */
	protected def variantModel = IssueVariantModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = VaryingIssueFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted varying issue
	  * @return An access point to that varying issue
	  */
	def apply(id: Int) = DbSingleVaryingIssue(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique varying issues.
	  * @return An access point to the varying issue that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueVaryingIssueAccess(mergeCondition(condition))
}

