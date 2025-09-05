package utopia.logos.database.access.text.placement

import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides text placement -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include text_placement.
  * @param model   A model used for accessing text placement database properties
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByTextPlacement[+A <: FilterableView[A]](wrapped: A, model: TextPlacementDbProps) 
	extends FilterTextPlacements[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}