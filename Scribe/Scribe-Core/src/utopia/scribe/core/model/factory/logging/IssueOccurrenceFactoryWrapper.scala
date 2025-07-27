package utopia.scribe.core.model.factory.logging

import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement IssueOccurrenceFactory by wrapping a 
  * IssueOccurrenceFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueOccurrenceFactoryWrapper[A <: IssueOccurrenceFactory[A], +Repr] 
	extends IssueOccurrenceFactory[Repr]
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
	
	override def withCaseId(caseId: Int) = mapWrapped { _.withCaseId(caseId) }
	override def withCount(count: Int) = mapWrapped { _.withCount(count) }
	override def withDetails(details: Model) = mapWrapped { _.withDetails(details) }
	override def withErrorMessages(errorMessages: Seq[String]) = mapWrapped { _.withErrorMessages(errorMessages) }
	override def withOccurrencePeriod(occurrencePeriod: Span[Instant]) = 
		mapWrapped { _.withOccurrencePeriod(occurrencePeriod) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

