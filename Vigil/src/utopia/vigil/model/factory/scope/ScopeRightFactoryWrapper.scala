package utopia.vigil.model.factory.scope

import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement ScopeRightFactory by wrapping a ScopeRightFactory 
  * instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightFactoryWrapper[A <: ScopeRightFactory[A], +Repr] extends ScopeRightFactory[Repr]
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
	
	override def withCreated(created: Instant) = mapWrapped { _.withCreated(created) }
	
	override def withScopeId(scopeId: Int) = mapWrapped { _.withScopeId(scopeId) }
	
	override def withUsable(usable: Boolean) = mapWrapped { _.withUsable(usable) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

