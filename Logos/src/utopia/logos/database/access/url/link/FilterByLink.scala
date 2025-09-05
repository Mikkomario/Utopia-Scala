package utopia.logos.database.access.url.link

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides link -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include link.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByLink[+A <: FilterableView[A]](wrapped: A) 
	extends FilterLinks[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}