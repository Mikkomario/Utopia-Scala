package utopia.scribe.api.database.access.logging.issue.variant

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides issue variant -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include issue_variant.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class FilterByIssueVariant[+A <: FilterableView[A]](wrapped: A) 
	extends FilterIssueVariants[A] with FilterableViewWrapper[A]

