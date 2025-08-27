package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.scribe.core.model.combined.management.DetailedResolution
import utopia.scribe.core.model.stored.logging.Issue
import utopia.scribe.core.model.stored.management.IssueAlias

object ManagedIssueInstances
{
	// ATTRIBUTES   ---------------------
	
	implicit val ord: Ordering[ManagedIssueInstances] = ManagedIssue.createOrdering
	
	
	// OTHER    -------------------------
	
	/**
	 * @param issue The issue to wrap, including variant & occurrence information
	 * @param aliasing Aliasing information to include
	 * @param resolutions Resolutions of this issue
	 * @return Issue with the specified information included
	 */
	def apply(issue: IssueInstances, aliasing: Option[IssueAlias],
	          resolutions: Seq[DetailedResolution]): ManagedIssueInstances =
		apply(issue.issue, aliasing, issue.variants, resolutions)
	/**
	 * @param issue The issue to wrap
	 * @param aliasing Aliasing information to include
	 * @param variants Information about this issue's variants and occurrences
	 * @param resolutions Resolutions of this issue
	 * @return Issue with the specified information included
	 */
	def apply(issue: Issue, aliasing: Option[IssueAlias] = None, variants: Seq[IssueVariantInstances] = Empty,
	          resolutions: Seq[DetailedResolution] = Empty): ManagedIssueInstances =
		_ManagedIssueInstances(issue, aliasing, variants, resolutions)
	
	
	// NESTED   -------------------------
	
	private case class _ManagedIssueInstances(issue: Issue, aliasing: Option[IssueAlias], variants: Seq[IssueVariantInstances],
	                                          resolutions: Seq[DetailedResolution])
		extends ManagedIssueInstances
	{
		override protected def wrap(factory: Issue): ManagedIssueInstances = copy(issue = factory)
	}
}

/**
 * Attaches various (but not full) management information to an issue instance
 *
 * @author Mikko Hilpinen
 * @since 27.08.2025, v1.2
 */
trait ManagedIssueInstances extends ManagedIssue with IssueInstances with CombinedIssue[ManagedIssueInstances]
