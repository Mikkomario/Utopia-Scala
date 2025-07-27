package utopia.scribe.api.database.reader.logging

import utopia.scribe.core.model.combined.logging.ContextualIssueVariant
import utopia.scribe.core.model.stored.logging.{Issue, IssueVariant}
import utopia.vault.nosql.read.linked.CombiningDbRowReader

/**
  * Used for reading contextual issue variants from the database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object ContextualIssueVariantDbReader 
	extends CombiningDbRowReader[IssueVariant, Issue, ContextualIssueVariant](IssueVariantDbReader, 
		IssueDbReader)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param variant variant to wrap
	  * @param issue   issue to attach to this variant
	  */
	override def combine(variant: IssueVariant, issue: Issue) = ContextualIssueVariant(variant, issue)
}

