package utopia.logos.database.access.text.statement.placement

import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides statement placement -based filtering for other types of access 
  * points.
  * @param wrapped Wrapped access point. Expected to include statement_placement.
  * @param model   A model used for accessing statement placement database properties
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByStatementPlacement[+A <: FilterableView[A]](wrapped: A, model: StatementPlacementDbProps) 
	extends FilterStatementPlacements[A] with FilterableViewWrapper[A]

