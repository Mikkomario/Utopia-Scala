package utopia.scribe.api.database.factory.logging

import utopia.scribe.core.model.combined.logging.VaryingIssue
import utopia.scribe.core.model.stored.logging.{Issue, IssueVariant}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory

/**
  * Used for reading varying issues from the database
  * @author Mikko Hilpinen
  * @since 26.05.2023, v0.1
  */
object VaryingIssueFactory extends MultiCombiningFactory[VaryingIssue, Issue, IssueVariant]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = IssueVariantFactory
	
	override def isAlwaysLinked = false
	
	override def parentFactory = IssueFactory
	
	override def apply(issue: Issue, variants: Seq[IssueVariant]) = VaryingIssue(issue, variants)
}

