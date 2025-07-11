package utopia.logos.database.access.url.path

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides request path -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include request_path.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByRequestPath[+A <: FilterableView[A]](wrapped: A) 
	extends FilterRequestPaths[A] with FilterableViewWrapper[A]

