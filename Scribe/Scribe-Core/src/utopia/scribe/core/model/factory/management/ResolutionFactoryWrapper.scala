package utopia.scribe.core.model.factory.management

import utopia.flow.util.{Mutate, Version}

import java.time.Instant

/**
  * Common trait for classes that implement ResolutionFactory by wrapping a ResolutionFactory 
  * instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait ResolutionFactoryWrapper[A <: ResolutionFactory[A], +Repr] extends ResolutionFactory[Repr]
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
	
	override def withCommentId(commentId: Int) = mapWrapped { _.withCommentId(commentId) }
	
	override def withCreated(created: Instant) = mapWrapped { _.withCreated(created) }
	
	override def withDeprecates(deprecates: Instant) = mapWrapped { _.withDeprecates(deprecates) }
	
	override def withNotifies(notifies: Boolean) = mapWrapped { _.withNotifies(notifies) }
	
	override def withResolvedIssueId(resolvedIssueId: Int) = 
		mapWrapped { _.withResolvedIssueId(resolvedIssueId) }
	
	override def withSilences(silences: Boolean) = mapWrapped { _.withSilences(silences) }
	
	override def withVersionThreshold(versionThreshold: Version) = 
		mapWrapped { _.withVersionThreshold(versionThreshold) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

