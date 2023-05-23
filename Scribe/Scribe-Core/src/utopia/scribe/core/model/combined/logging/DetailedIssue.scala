package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue

/**
  * Adds detailed information about an issue's variant(s) to an issue
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  * @constructor Combines an issue with its variants
  * @param issue the issue to wrap
  * @param variants The variants of this issue, including detailed information
  */
case class DetailedIssue(issue: Issue, variants: Vector[DetailedIssueVariant]) extends Extender[IssueData]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Id of this issue
	  */
	def id = issue.id
	
	
	// IMPLEMENTED  --------------------------
	
	override def wrapped: IssueData = issue.data
}
