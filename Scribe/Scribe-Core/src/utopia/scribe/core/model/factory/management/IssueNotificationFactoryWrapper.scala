package utopia.scribe.core.model.factory.management

import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement IssueNotificationFactory by wrapping a 
  * IssueNotificationFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait IssueNotificationFactoryWrapper[A <: IssueNotificationFactory[A], +Repr] 
	extends IssueNotificationFactory[Repr]
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
	
	override def withClosed(closed: Instant) = mapWrapped { _.withClosed(closed) }
	
	override def withCreated(created: Instant) = mapWrapped { _.withCreated(created) }
	
	override def withResolutionId(resolutionId: Int) = mapWrapped { _.withResolutionId(resolutionId) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

