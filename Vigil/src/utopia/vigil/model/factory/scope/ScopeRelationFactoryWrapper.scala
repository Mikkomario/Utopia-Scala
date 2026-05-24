package utopia.vigil.model.factory.scope

import utopia.flow.util.Mutate

/**
  * Common trait for classes that implement ScopeRelationFactory by wrapping a 
  * ScopeRelationFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
trait ScopeRelationFactoryWrapper[A <: ScopeRelationFactory[A], +Repr] extends ScopeRelationFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The factory wrapped by this instance
	  */
	protected def wrappedFactory: A
	
	/**
	  * Mutates this item by wrapping a mutated instance
	  * @param factory The new factory instance to wrap
	  * @return Copy of this item with the specified wrapped factory
	  */
	protected def wrap(factory: A): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def withGrantedScopeId(grantedScopeId: Int) = mapWrapped { _.withGrantedScopeId(grantedScopeId) }
	
	override def withParentScopeId(parentScopeId: Int) = mapWrapped { _.withParentScopeId(parentScopeId) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

