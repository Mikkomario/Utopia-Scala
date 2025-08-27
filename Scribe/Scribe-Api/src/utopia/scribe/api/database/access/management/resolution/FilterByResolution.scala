package utopia.scribe.api.database.access.management.resolution

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides resolution -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include issue_resolution.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class FilterByResolution[+A <: FilterableView[A]](wrapped: A) 
	extends FilterResolutions[A] with FilterableViewWrapper[A]

