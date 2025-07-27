package utopia.scribe.api.database.access.many.logging.issue

import utopia.vault.nosql.view.FilterableView
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.model.logging.IssueModel

import java.time.Instant

/**
  * Common trait for access points that can be filtered / targeted based on issue properties
  * NB: Currently this trait is a proof of concept. It is not intended for production use.
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.0.5
  */
@deprecated("Replaced with targeting access classes", "v1.2")
trait FilterIssues[+Repr] extends FilterableView[Repr]
{
	// COMPUTED -------------------------
	
	/**
	  * @return Model used for interacting with issue-specific DB properties
	  */
	protected def issueModel = IssueModel
	
	
	// OTHER    -------------------------
	
	/**
	  * @param context Targeted issue context
	  * @param includeSubContexts Whether contexts appearing under the specified context should also be included
	  * (default = false)
	  * @return Access to issues with that context
	  */
	def inContext(context: String, includeSubContexts: Boolean = false) = {
		val condition = {
			if (includeSubContexts)
				issueModel.contextColumn.startsWith(context)
			else
				issueModel.withContext(context).toCondition
		}
		filter(condition)
	}
	/**
	  * @param contextPart Context or part of a context to search for
	  * @return Access to issues that include the specified string in their context
	  */
	def includingContext(contextPart: String) =
		if (contextPart.isEmpty) self else filter(issueModel.contextColumn.contains(contextPart))
	
	/**
	  * @param threshold A time threshold
	  * @return Access to issues that appeared since the specified time threshold
	  */
	def appearedSince(threshold: Instant) = filter(issueModel.createdColumn >= threshold)
}
