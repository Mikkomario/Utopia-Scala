package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.scribe.core.model.combined.management.DetailedResolution
import utopia.scribe.core.model.stored.logging.Issue
import utopia.scribe.core.model.stored.management.{Comment, IssueAlias}

/**
 * Includes variant and occurrence information, as well as management-related information to an issue
 * @param issue Wrapped issue
 * @param variants Variants of this issue, including occurrence and error data
 * @param aliasing Alias given to this issue, if applicable
 * @param comments Comments about this issue
 * @param resolutions Resolutions of this issue, including possible notifications
 * @author Mikko Hilpinen
 * @since 26.08.2025, v1.2
 */
case class DetailedIssue(issue: Issue, variants: Seq[DetailedIssueVariant] = Empty, aliasing: Option[IssueAlias] = None,
                         comments: Seq[Comment] = Empty, resolutions: Seq[DetailedResolution] = Empty)
	extends IssueWithDetailedVariants with CombinedIssue[DetailedIssue]
{
	override protected def wrap(factory: Issue): DetailedIssue = copy(issue = factory)
}