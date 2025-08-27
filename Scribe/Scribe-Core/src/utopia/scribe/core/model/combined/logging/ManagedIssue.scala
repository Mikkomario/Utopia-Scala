package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.util.Version
import utopia.flow.view.immutable.View
import utopia.scribe.core.model.combined.management.DetailedResolution
import utopia.scribe.core.model.stored.logging.Issue
import utopia.scribe.core.model.stored.management.IssueAlias

object ManagedIssue
{
	// ATTRIBUTES   ---------------------
	
	implicit val ord: Ordering[ManagedIssue] = createOrdering[ManagedIssue]
	
	
	// COMPUTED -------------------------
	
	/**
	 * Creates an ordering to use with managed issues of some kind
	 * @tparam I Type of ordered items
	 * @return A new ordering
	 */
	def createOrdering[I <: ManagedIssue] = CombinedOrdering[I](
		Ordering.by { _.severity }, Ordering.by { _.context }, Ordering.by { _.created })
	
	
	// OTHER    -------------------------
	
	/**
	 * @param issue Issue to wrap
	 * @param aliasing Aliasing to apply
	 * @param resolutions Resolutions that apply to this issue
	 * @return Combination of the specified information
	 */
	def apply(issue: Issue, aliasing: Option[IssueAlias] = None,
	          resolutions: Seq[DetailedResolution] = Empty): ManagedIssue =
		_ManagedIssue(issue, aliasing, resolutions)
	
	
	// NESTED   -------------------------
	
	private case class _ManagedIssue(issue: Issue, aliasing: Option[IssueAlias], resolutions: Seq[DetailedResolution])
		extends ManagedIssue
	{
		override protected def wrap(factory: Issue): ManagedIssue = copy(issue = factory)
	}
}

/**
 * Attaches various (but not full) management information to an issue instance
 * @author Mikko Hilpinen
 * @since 27.08.2025, v1.2
 */
trait ManagedIssue extends CombinedIssue[ManagedIssue]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Aliasing information for this issue
	 */
	def aliasing: Option[IssueAlias]
	/**
	 * @return Resolutions given to this issue, including possible notifications and linked comments
	 */
	def resolutions: Seq[DetailedResolution]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return The severity level of this issue, with aliasing modifier applied, if one is present
	 */
	def severity = aliasing.flatMap { _.newSeverity }.getOrElse(issue.severity)
	
	/**
	 * @return Alias of this issue. Empty if no alias has been given.
	 */
	def alias = aliasing match {
		case Some(alias) => alias.alias
		case None => ""
	}
	/**
	 * @return Alias given to this issue, or its context
	 */
	def aliasOrContext = aliasing.map { _.alias }.filter { _.nonEmpty }.getOrElse(issue.context)
	
	/**
	 * @return Unread notifications concerning this issue
	 */
	def unreadNotifications =
		resolutions.view.flatMap { r => r.notification.filter { _.isValid }.map { r -> _ } }.toOptimizedSeq
	/**
	 * @return Whether this issue contains unread notifications
	 */
	def hasUnreadNotifications = resolutions.exists { _.notification.exists { _.isValid } }
	
	/**
	 * @return Whether this issue has been unconditionally silenced
	 */
	def isAlwaysSilenced = resolutions.exists { r => r.silences && r.isValid && r.versionThreshold.isEmpty }
	/**
	 * @return Whether this issue is silenced only in certain versions
	 */
	def isConditionallySilenced = {
		val silencingResolutionsIter = resolutions.iterator.filter { r => r.silences && r.isValid }
		silencingResolutionsIter.hasNext && silencingResolutionsIter.forall { _.versionThreshold.isDefined }
	}
	
	
	// OTHER    ------------------------
	
	/**
	 * @param lazyVersion Lazily accessible current target app version
	 * @return Whether this issue has been silenced in the specified app version
	 */
	def isSilencedInVersion(lazyVersion: View[Version]) = resolutions.exists { _.silencesVersion(lazyVersion.value) }
}
