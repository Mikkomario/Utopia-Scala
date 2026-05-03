package utopia.vigil.model.factory.token

import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement TokenFactory by wrapping a TokenFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenFactoryWrapper[A <: TokenFactory[A], +Repr] extends TokenFactory[Repr]
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
	
	override def withExpires(expires: Instant) = mapWrapped { _.withExpires(expires) }
	
	override def withHash(hash: String) = mapWrapped { _.withHash(hash) }
	
	override def withName(name: String) = mapWrapped { _.withName(name) }
	
	override def withParentId(parentId: Int) = mapWrapped { _.withParentId(parentId) }
	
	override def withRevoked(revoked: Instant) = mapWrapped { _.withRevoked(revoked) }
	
	override def withTemplateId(templateId: Int) = mapWrapped { _.withTemplateId(templateId) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

