package utopia.reflection.component.template.layout.stack

import utopia.reflection.shape.stack.modifier.StackSizeModifier

/**
  * Common trait for wrappers that wrap a constrainable instance
  * @author Mikko Hilpinen
  * @since 15.3.2020, v1
  */
trait ConstrainableWrapper extends Constrainable
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Wrapped constrainable instance
	  */
	protected def wrapped: Constrainable
	
	
	// IMPLEMENTED	-------------------
	
	override def constraints = wrapped.constraints
	
	override def constraints_=(newConstraints: Vector[StackSizeModifier]) = wrapped.constraints = newConstraints
	
	override def calculatedStackSize = wrapped.calculatedStackSize
}
