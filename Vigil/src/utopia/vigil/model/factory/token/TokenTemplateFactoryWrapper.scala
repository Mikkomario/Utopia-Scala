package utopia.vigil.model.factory.token

import utopia.flow.time.Duration
import utopia.flow.util.Mutate
import utopia.vigil.model.enumeration.ScopeGrantType

import java.time.Instant

/**
  * Common trait for classes that implement TokenTemplateFactory by wrapping a 
  * TokenTemplateFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenTemplateFactoryWrapper[A <: TokenTemplateFactory[A], +Repr] extends TokenTemplateFactory[Repr]
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
	
	override def withDuration(duration: Duration) = mapWrapped { _.withDuration(duration) }
	
	override def withName(name: String) = mapWrapped { _.withName(name) }
	
	override def withScopeGrantType(scopeGrantType: ScopeGrantType) = 
		mapWrapped { _.withScopeGrantType(scopeGrantType) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

