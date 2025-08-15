package utopia.vault.nosql.view

import utopia.vault.model.immutable.Column

/**
 * Provides filtering functions based on a timestamp column
 *
 * @author Mikko Hilpinen
 * @since 15.08.2025, v2.0
 */
case class FilterByTimestamp[+V <: FilterableView[V]](wrapped: V, timestampColumn: Column)
	extends TimelineView[V] with FilterableViewWrapper[V]