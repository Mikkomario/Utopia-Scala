package utopia.scribe.core.model.factory.logging

import utopia.flow.util.Mutate

/**
  * Common trait for classes that implement ErrorRecordFactory by wrapping a ErrorRecordFactory 
  * instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait ErrorRecordFactoryWrapper[A <: ErrorRecordFactory[A], +Repr] extends ErrorRecordFactory[Repr]
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
	
	override def withCauseId(causeId: Int) = mapWrapped { _.withCauseId(causeId) }
	
	override def withExceptionType(exceptionType: String) = mapWrapped { _.withExceptionType(exceptionType) }
	
	override def withStackTraceId(stackTraceId: Int) = mapWrapped { _.withStackTraceId(stackTraceId) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

