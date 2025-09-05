package utopia.vault.nosql.view

import utopia.vault.model.immutable.{Column, Table, TableColumn}

/**
 * Provides filtering functions based on a timestamp column
 *
 * @author Mikko Hilpinen
 * @since 15.08.2025, v2.0
 */
case class FilterByTimestamp[+V <: FilterableView[V]](wrapped: V, timestamp: TableColumn)
	extends TimelineView[V] with FilterableViewWrapper[V]
{
	override def table: Table = timestamp.table
	override protected def timestampColumn: Column = timestamp.column
}