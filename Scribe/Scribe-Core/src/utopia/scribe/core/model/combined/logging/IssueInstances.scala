package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue

/**
  * Lists variants and occurrences of an issue, along with the base issue data
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
case class IssueInstances(issue: Issue, variants: Vector[IssueVariantInstances] = Vector()) extends Extender[IssueData]
{
	// COMPUTED ----------------------
	
	/**
	  * @return The DB id of this issue
	  */
	def id = issue.id
	
	
	// IMPLEMENTED  ------------------
	
	override def wrapped: IssueData = issue.data
}
