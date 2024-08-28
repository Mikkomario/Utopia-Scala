package utopia.logos.model.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement LinkFactory by wrapping a LinkFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait LinkFactoryWrapper[A <: LinkFactory[A], +Repr] extends LinkFactory[Repr]
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
	
	override def withPathId(pathId: Int) = mapWrapped { _.withPathId(pathId) }
	
	override def withQueryParameters(queryParameters: Model) = 
		mapWrapped { _.withQueryParameters(queryParameters) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

