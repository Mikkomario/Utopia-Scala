package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.factory.logging.VaryingIssueFactory
import utopia.scribe.api.database.model.logging.IssueVariantModel
import utopia.scribe.core.model.combined.logging.VaryingIssue
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueVaryingIssueAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueVaryingIssueAccess = new _UniqueVaryingIssueAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueVaryingIssueAccess(condition: Condition) extends UniqueVaryingIssueAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return distinct varying issues
  * @author Mikko Hilpinen
  * @since 26.05.2023, v0.1
  */
trait UniqueVaryingIssueAccess 
	extends UniqueIssueAccessLike[VaryingIssue] with FilterableView[UniqueVaryingIssueAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with the linked variants
	  */
	protected def variantModel = IssueVariantModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = VaryingIssueFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueVaryingIssueAccess = UniqueVaryingIssueAccess(condition)
}

