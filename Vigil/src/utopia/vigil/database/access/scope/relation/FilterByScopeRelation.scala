package utopia.vigil.database.access.scope.relation

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides scope relation -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include scope_relation.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class FilterByScopeRelation[+A <: FilterableView[A]](wrapped: A) 
	extends FilterScopeRelations[A] with FilterableViewWrapper[A]
{
	// IMPLEMENTED	--------------------
	
	override def table = model.table
}

