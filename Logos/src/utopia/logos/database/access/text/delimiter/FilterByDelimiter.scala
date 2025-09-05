package utopia.logos.database.access.text.delimiter

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides delimiter -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include delimiter.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByDelimiter[+A <: FilterableView[A]](wrapped: A) 
	extends FilterDelimiters[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}