package utopia.logos.database.access.url.link.placement

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides link placement -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include link_placement.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByLinkPlacement[+A <: FilterableView[A]](wrapped: A) 
	extends FilterLinkPlacements[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}
