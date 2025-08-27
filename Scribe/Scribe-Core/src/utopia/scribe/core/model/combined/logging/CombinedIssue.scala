package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.factory.logging.IssueFactoryWrapper
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.store.HasId

/**
 * Common trait for classes which attach other data to Issues
 *
 * @author Mikko Hilpinen
 * @since 26.08.2025, v1.2
 */
trait CombinedIssue[+Repr] extends Extender[IssueData] with HasId[Int] with IssueFactoryWrapper[Issue, Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The wrapped issue instance
	 */
	def issue: Issue
	
	
	// IMPLEMENTED  --------------------
	
	override def id: Int = issue.id
	
	override def wrapped: IssueData = issue.data
	override protected def wrappedFactory: Issue = issue
}
