package utopia.scribe.api.database.access.logging.issue

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
}

