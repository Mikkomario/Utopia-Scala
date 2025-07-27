package utopia.scribe.api.database.reader.logging

import utopia.scribe.core.model.combined.logging.VaryingIssue
import utopia.scribe.core.model.stored.logging.{Issue, IssueVariant}
import utopia.vault.nosql.read.linked.MultiLinkedDbReader

/**
  * Used for reading varying issues from the database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object VaryingIssueDbReader 
	extends MultiLinkedDbReader[Issue, IssueVariant, VaryingIssue](IssueDbReader, IssueVariantDbReader, 
		neverEmptyRight = false)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param issue    issue to wrap
	  * @param variants variants to attach to this issue
	  */
	override def combine(issue: Issue, variants: Seq[IssueVariant]) = VaryingIssue(issue, variants)
}

