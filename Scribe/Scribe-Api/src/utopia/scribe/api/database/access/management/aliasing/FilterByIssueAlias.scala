package utopia.scribe.api.database.access.management.aliasing

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides issue alias -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include issue_alias.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class FilterByIssueAlias[+A <: FilterableView[A]](wrapped: A) 
	extends FilterIssueAliases[A] with FilterableViewWrapper[A]

