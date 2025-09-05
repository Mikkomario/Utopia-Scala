package utopia.vault.test.database.access.item.versioned

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides versioned test item -based filtering for other types of access 
  * points.
  * @param wrapped Wrapped access point. Expected to include versioned_test_item.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class FilterByVersionedTestItem[+A <: FilterableView[A]](wrapped: A) 
	extends FilterTestItems[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}