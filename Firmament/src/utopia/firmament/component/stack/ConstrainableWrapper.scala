package utopia.firmament.component.stack

import utopia.firmament.model.stack.modifier.StackSizeModifier

/**
  * Common trait for wrappers that wrap a constrainable instance
  * @author Mikko Hilpinen
  * @since 15.3.2020, Reflection v1
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
	override def constraints_=(newConstraints: Seq[StackSizeModifier]) = wrapped.constraints = newConstraints
	
	override def calculatedStackSize = wrapped.calculatedStackSize
}
