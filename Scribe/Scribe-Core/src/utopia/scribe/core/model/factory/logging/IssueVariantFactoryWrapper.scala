package utopia.scribe.core.model.factory.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.{Mutate, Version}

import java.time.Instant

/**
  * Common trait for classes that implement IssueVariantFactory by wrapping a IssueVariantFactory 
  * instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueVariantFactoryWrapper[A <: IssueVariantFactory[A], +Repr] extends IssueVariantFactory[Repr]
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
	
	override def withDetails(details: Model) = mapWrapped { _.withDetails(details) }
	
	override def withErrorId(errorId: Int) = mapWrapped { _.withErrorId(errorId) }
	
	override def withIssueId(issueId: Int) = mapWrapped { _.withIssueId(issueId) }
	
	override def withVersion(version: Version) = mapWrapped { _.withVersion(version) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

