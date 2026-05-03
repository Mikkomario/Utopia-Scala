package utopia.vigil.database.access.token.template.right

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides token template scope -based filtering for other types of access 
  * points.
  * @param wrapped Wrapped access point. Expected to include token_template_scope.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class FilterByTokenTemplateScope[+A <: FilterableView[A]](wrapped: A) 
	extends FilterTokenTemplateScopes[A] with FilterableViewWrapper[A]
{
	// IMPLEMENTED	--------------------
	
	override def table = model.table
}

