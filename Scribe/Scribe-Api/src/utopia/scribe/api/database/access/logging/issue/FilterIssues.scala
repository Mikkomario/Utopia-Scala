package utopia.scribe.api.database.access.logging.issue

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.IssueDbModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on issue properties
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
trait FilterIssues[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines issue database properties
	  */
	def model = IssueDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param context context to target
	  * @return Copy of this access point that only includes issues with the specified context
	  */
	def inContext(context: String) = filter(model.context.column <=> context)
	/**
	  * @param contexts Targeted contexts
	  * @return Copy of this access point that only includes issues where context is within the specified 
	  * value set
	  */
	def inContexts(contexts: Iterable[String]) = filter(model.context.column.in(contexts))
	/**
	  * @param context Targeted issue context
	  * @param includeSubContexts Whether contexts appearing under the specified context should also be included
	  * (default = false)
	  * @return Access to issues with that context
	  */
	def inContext(context: String, includeSubContexts: Boolean): Repr = {
		if (includeSubContexts)
			inContext(context)
		else
			filter(model.context.column.startsWith(context))
	}
	
	/**
	  * @param contextPart Context or part of a context to search for
	  * @return Access to issues that include the specified string in their context
	  */
	def includingContext(contextPart: String) =
		if (contextPart.isEmpty) self else filter(model.context.column.contains(contextPart))
	
	/**
	  * @param severities Targeted severities
	  * @return Copy of this access point that only includes issues where severity is within the specified 
	  * value set
	  */
	def ofSeverities(severities: Iterable[Severity]) = 
		filter(model.severity.column.in(severities.map { severity => severity.level }))
	/**
	  * @param severity severity to target
	  * @return Copy of this access point that only includes issues with the specified severity
	  */
	def ofSeverity(severity: Severity) = filter(model.severity.column <=> severity.level)
	
	/**
	  * @param minSeverity Smallest included issue severity
	  * @return Access to issues that are of the specified severity level or higher
	  */
	def withSeverityAtLeast(minSeverity: Severity) = {
		if (minSeverity == Severity.min)
			self
		else
			filter(model.severity >= minSeverity)
	}
	/**
	  * @param maxSeverity Largest included issue severity
	  * @return Access to issues with that severity or lower
	  */
	def withSeverityNoMoreThan(maxSeverity: Severity) = {
		if (maxSeverity == Severity.max)
			self
		else
			filter(model.severity <= maxSeverity)
	}
	/**
	  * @param severityRange Targeted range of issue severities. Must be ascending.
	  * @return Access to issues within that range of severities
	  */
	def withSeverityIn(severityRange: HasInclusiveEnds[Severity]) = {
		// Case: Single severity targeted
		if (severityRange.isUnit)
			ofSeverity(severityRange.start)
		// Case: No lower bound
		else if (severityRange.start == Severity.min)
			withSeverityNoMoreThan(severityRange.end)
		// Case: No upper bound
		else if (severityRange.end == Severity.max)
			withSeverityAtLeast(severityRange.start)
		// Case: Lower and upper bound specified
		else
			filter(model.severity.isBetween(severityRange.start, severityRange.end))
	}
}

