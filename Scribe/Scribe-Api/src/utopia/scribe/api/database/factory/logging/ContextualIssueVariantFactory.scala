package utopia.scribe.api.database.factory.logging

import utopia.scribe.core.model.combined.logging.ContextualIssueVariant
import utopia.scribe.core.model.stored.logging.{Issue, IssueVariant}
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading contextual issue variants from the database
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
object ContextualIssueVariantFactory 
	extends CombiningFactory[ContextualIssueVariant, IssueVariant, Issue] 
		with FromRowFactoryWithTimestamps[ContextualIssueVariant]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = IssueFactory
	
	override def creationTimePropertyName = IssueVariantFactory.creationTimePropertyName
	
	override def parentFactory = IssueVariantFactory
	
	override def apply(variant: IssueVariant, issue: Issue) = ContextualIssueVariant(variant, issue)
}

