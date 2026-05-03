package utopia.vigil.database.access.scope

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides scope -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include scope.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class FilterByScope[+A <: FilterableView[A]](wrapped: A) 
	extends FilterScopes[A] with FilterableViewWrapper[A]
{
	// IMPLEMENTED	--------------------
	
	override def table = model.table
}

