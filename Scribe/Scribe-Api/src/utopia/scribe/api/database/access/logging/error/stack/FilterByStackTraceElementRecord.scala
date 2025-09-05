package utopia.scribe.api.database.access.logging.error.stack

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides stack trace element record -based filtering for other types of 
  * access points.
  * @param wrapped Wrapped access point. Expected to include stack_trace_element_record.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
case class FilterByStackTraceElementRecord[+A <: FilterableView[A]](wrapped: A) 
	extends FilterStackTraceElementRecords[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}