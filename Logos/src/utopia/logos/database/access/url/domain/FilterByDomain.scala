package utopia.logos.database.access.url.domain

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides domain -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include domain.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByDomain[+A <: FilterableView[A]](wrapped: A) 
	extends FilterDomains[A] with FilterableViewWrapper[A]

