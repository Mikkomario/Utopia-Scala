package utopia.reflection.shape.stack.modifier

import utopia.reflection.shape.stack.StackLength

/**
  * This constraint preserves the longest encountered length (based on optimal length)
  * @author Mikko Hilpinen
  * @since 30.8.2020, v1.2.1
  * @param startLength Initial length value (default = flexible 0)
  */
class NoShrinkingConstraint(startLength: StackLength = StackLength.any.expanding) extends StackLengthModifier
{
	// ATTRIBUTES   ----------------------
	
	private var currentLength = startLength
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(length: StackLength) =
	{
		if (length.optimal > currentLength.optimal)
			currentLength = length
		
		currentLength
	}
}
