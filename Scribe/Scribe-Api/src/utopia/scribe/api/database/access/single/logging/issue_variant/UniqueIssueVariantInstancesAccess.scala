package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.api.database.factory.logging.IssueVariantInstancesFactory
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueIssueVariantInstancesAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueIssueVariantInstancesAccess = 
		new _UniqueIssueVariantInstancesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueIssueVariantInstancesAccess(condition: Condition) 
		extends UniqueIssueVariantInstancesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return distinct issue variant instances
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
trait UniqueIssueVariantInstancesAccess 
	extends UniqueIssueVariantAccessLike[IssueVariantInstances] 
		with FilterableView[UniqueIssueVariantInstancesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with the linked occurrences
	  */
	protected def occurrenceModel = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantInstancesFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueIssueVariantInstancesAccess = 
		new UniqueIssueVariantInstancesAccess._UniqueIssueVariantInstancesAccess(mergeCondition(filterCondition))
}

