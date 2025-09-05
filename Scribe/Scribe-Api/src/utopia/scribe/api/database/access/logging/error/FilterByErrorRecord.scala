package utopia.scribe.api.database.access.logging.error

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides error record -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include error_record.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
case class FilterByErrorRecord[+A <: FilterableView[A]](wrapped: A) 
	extends FilterErrorRecords[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}