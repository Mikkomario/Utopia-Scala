package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points that apply severity-based filtering.
  * Practically only applicable to access points that include the Issue table
  * @author Mikko Hilpinen
  * @since 10.7.2023, v1.0
  */
trait SeverityBasedAccess[+Repr] extends FilterableView[Repr]
{
	// COMPUTED -------------------------
	
	/**
	  * @return Model used for interacting with issue data
	  */
	protected def issueModel = IssueModel
	
	
	// OTHER    -------------------------
	
	/**
	  * @param severity Targeted severity level
	  * @return Access to issues of that severity level
	  */
	def withSeverity(severity: Severity) = filter(issueModel.withSeverity(severity).toCondition)
	
	/**
	  * @param minSeverity Smallest included issue severity
	  * @return Access to issues that are of the specified severity level or higher
	  */
	def withSeverityAtLeast(minSeverity: Severity) = {
		if (minSeverity == Severity.min)
			self
		else
			filter(issueModel.severityColumn >= minSeverity)
	}
	
	/**
	  * @param maxSeverity Largest included issue severity
	  * @return Access to issues with that severity or lower
	  */
	def withSeverityNoMoreThan(maxSeverity: Severity) = {
		if (maxSeverity == Severity.max)
			self
		else
			filter(issueModel.severityColumn <= maxSeverity)
	}
	
	/**
	  * @param severityRange Targeted range of issue severities. Must be ascending.
	  * @return Access to issues within that range of severities
	  */
	def withSeverityIn(severityRange: HasInclusiveEnds[Severity]) = {
		// Case: Single severity targeted
		if (severityRange.isUnit)
			withSeverity(severityRange.start)
		// Case: No lower bound
		else if (severityRange.start == Severity.min)
			withSeverityNoMoreThan(severityRange.end)
		// Case: No upper bound
		else if (severityRange.end == Severity.max)
			withSeverityAtLeast(severityRange.start)
		// Case: Lower and upper bound specified
		else
			filter(issueModel.severityColumn.isBetween(severityRange.start, severityRange.end))
	}
}
