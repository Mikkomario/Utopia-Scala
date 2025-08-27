package utopia.scribe.core.model.factory.management

import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement CommentFactory by wrapping a CommentFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait CommentFactoryWrapper[A <: CommentFactory[A], +Repr] extends CommentFactory[Repr]
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
	
	override def withIssueVariantId(issueVariantId: Int) = mapWrapped { _.withIssueVariantId(issueVariantId) }
	
	override def withText(text: String) = mapWrapped { _.withText(text) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

