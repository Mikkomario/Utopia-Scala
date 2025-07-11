package utopia.logos.model.factory.text

import utopia.flow.util.Mutate

/**
  * Common trait for classes that implement TextPlacementFactory by wrapping a TextPlacementFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementFactoryWrapper[A <: TextPlacementFactory[A], +Repr] extends TextPlacementFactory[Repr]
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
	
	override def withOrderIndex(orderIndex: Int) = mapWrapped { _.withOrderIndex(orderIndex) }
	
	override def withParentId(parentId: Int) = mapWrapped { _.withParentId(parentId) }
	
	override def withPlacedId(placedId: Int) = mapWrapped { _.withPlacedId(placedId) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

