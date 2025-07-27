package utopia.scribe.api.database.access.logging.issue

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides issue -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include issue.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class FilterByIssue[+A <: FilterableView[A]](wrapped: A) 
	extends FilterIssues[A] with FilterableViewWrapper[A]

