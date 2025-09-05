package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides issue occurrence -based filtering for other types of access 
  * points.
  * @param wrapped Wrapped access point. Expected to include issue_occurrence.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class FilterByIssueOccurrence[+A <: FilterableView[A]](wrapped: A) 
	extends FilterIssueOccurrences[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}