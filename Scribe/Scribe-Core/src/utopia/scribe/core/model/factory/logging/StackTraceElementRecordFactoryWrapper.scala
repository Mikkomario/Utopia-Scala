package utopia.scribe.core.model.factory.logging

import utopia.flow.util.Mutate

/**
  * Common trait for classes that implement StackTraceElementRecordFactory by wrapping a 
  * StackTraceElementRecordFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait StackTraceElementRecordFactoryWrapper[A <: StackTraceElementRecordFactory[A], +Repr] 
	extends StackTraceElementRecordFactory[Repr]
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
	
	override def withClassName(className: String) = mapWrapped { _.withClassName(className) }
	
	override def withFileName(fileName: String) = mapWrapped { _.withFileName(fileName) }
	
	override def withLineNumber(lineNumber: Int) = mapWrapped { _.withLineNumber(lineNumber) }
	
	override def withMethodName(methodName: String) = mapWrapped { _.withMethodName(methodName) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

