package utopia.vigil.database.access.scope.right

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}
import utopia.vigil.database.props.scope.ScopeRightDbProps

/**
  * An interface which provides scope right -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include scope_right.
  * @param model   A model used for accessing scope right database properties
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class FilterByScopeRight[+A <: FilterableView[A]](wrapped: A, model: ScopeRightDbProps) 
	extends FilterScopeRights[A] with FilterableViewWrapper[A]
{
	// IMPLEMENTED	--------------------
	
	override def table = model.table
}

