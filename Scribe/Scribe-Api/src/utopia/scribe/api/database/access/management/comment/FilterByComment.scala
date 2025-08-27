package utopia.scribe.api.database.access.management.comment

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides comment -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include issue_comment.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class FilterByComment[+A <: FilterableView[A]](wrapped: A) 
	extends FilterComments[A] with FilterableViewWrapper[A]

