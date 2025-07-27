package utopia.scribe.core.model.factory.logging

import utopia.flow.util.Mutate
import utopia.scribe.core.model.enumeration.Severity

import java.time.Instant

/**
  * Common trait for classes that implement IssueFactory by wrapping a IssueFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueFactoryWrapper[A <: IssueFactory[A], +Repr] extends IssueFactory[Repr]
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
	
	override def withContext(context: String) = mapWrapped { _.withContext(context) }
	
	override def withCreated(created: Instant) = mapWrapped { _.withCreated(created) }
	
	override def withSeverity(severity: Severity) = mapWrapped { _.withSeverity(severity) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

