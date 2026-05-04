package utopia.vigil.model.factory.token

import utopia.flow.util.{Mutate, UncertainBoolean}

/**
  * Common trait for classes that implement TokenGrantRightFactory by wrapping a 
  * TokenGrantRightFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
trait TokenGrantRightFactoryWrapper[A <: TokenGrantRightFactory[A], +Repr] 
	extends TokenGrantRightFactory[Repr]
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
	
	override def withGrantedTemplateId(grantedTemplateId: Int) = 
		mapWrapped { _.withGrantedTemplateId(grantedTemplateId) }
	
	override def withOwnerTemplateId(ownerTemplateId: Int) = 
		mapWrapped { _.withOwnerTemplateId(ownerTemplateId) }
	
	override def withRevokesEarlier(revokesEarlier: UncertainBoolean) = 
		mapWrapped { _.withRevokesEarlier(revokesEarlier) }
	
	override def withRevokesOriginal(revokesOriginal: Boolean) = 
		mapWrapped { _.withRevokesOriginal(revokesOriginal) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

