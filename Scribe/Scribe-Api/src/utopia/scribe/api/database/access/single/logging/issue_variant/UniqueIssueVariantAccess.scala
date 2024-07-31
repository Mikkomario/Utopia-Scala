package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.sql.Condition

object UniqueIssueVariantAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueIssueVariantAccess = new _UniqueIssueVariantAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueIssueVariantAccess(condition: Condition) extends UniqueIssueVariantAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct issue variants.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueIssueVariantAccess 
	extends UniqueIssueVariantAccessLike[IssueVariant] 
		with SingleChronoRowModelAccess[IssueVariant, UniqueIssueVariantAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueIssueVariantAccess = UniqueIssueVariantAccess(condition)
}

