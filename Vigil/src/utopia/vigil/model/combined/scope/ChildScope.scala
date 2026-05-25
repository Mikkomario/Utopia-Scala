package utopia.vigil.model.combined.scope

import utopia.vigil.model.factory.scope.ScopeFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeData
import utopia.vigil.model.stored.scope.{Scope, ScopeRelation}

object ChildScope
{
	// OTHER	--------------------
	
	/**
	  * @param scope        The scope to wrap
	  * @param linkToParent link to parent to attach
	  * @return Combination of the specified scope and link to parent
	  */
	def apply(scope: Scope, linkToParent: ScopeRelation): ChildScope = apply(scope.id, scope.data, 
		linkToParent)
	
	/**
	  * @param id           ID of this scope in the DB
	  * @param data         The wrapped scope data
	  * @param linkToParent link to parent to attach
	  * @return A new scope with the specified link to parent included
	  */
	def apply(id: Int, data: ScopeData, linkToParent: ScopeRelation): ChildScope = 
		_ChildScope(id, data, linkToParent)
	
	
	// NESTED	--------------------
	
	/**
	  * @param id           ID of this scope in the DB
	  * @param data         The wrapped scope data
	  * @param linkToParent link to parent to attach
	  */
	private case class _ChildScope(id: Int, data: ScopeData, linkToParent: ScopeRelation) extends ChildScope
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(factory: ScopeData) = copy(data = factory)
	}
}

/**
  * Represents a scope that's granted by some other scope
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
trait ChildScope extends Scope with ScopeFactoryWrapper[ScopeData, ChildScope]
{
	// ABSTRACT	--------------------
	
	/**
	  * The link to parent that is attached to this scope
	  */
	def linkToParent: ScopeRelation
}

