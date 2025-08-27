package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.scribe.core.model.stored.logging.Issue

object IssueWithDetailedVariants
{
	// OTHER    ----------------------------
	
	/**
	 * @param issue Issue to wrap
	 * @param variants Variants to include
	 * @return The specified issue with the specified variants included
	 */
	def apply(issue: Issue, variants: Seq[DetailedIssueVariant] = Empty): IssueWithDetailedVariants =
		_IssueWithDetailedVariants(issue, variants)
	
	
	// NESTED   ----------------------------
	
	private case class _IssueWithDetailedVariants(issue: Issue, variants: Seq[DetailedIssueVariant])
		extends IssueWithDetailedVariants
	{
		override protected def wrap(factory: Issue): IssueWithDetailedVariants = copy(issue = factory)
	}
}

/**
  * Adds detailed information about an issue's variant(s) to an issue
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
trait IssueWithDetailedVariants extends IssueInstances with CombinedIssue[IssueWithDetailedVariants]
{
	// ABSTRACT ------------------------------
	
	def variants: Seq[DetailedIssueVariant]
}
