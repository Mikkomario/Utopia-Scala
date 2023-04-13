package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackLength

/**
  * This constraint preserves the longest encountered length (based on optimal length)
  * @author Mikko Hilpinen
  * @since 30.8.2020, Reflection v1.2.1
  * @param startLength Initial length value (default = flexible 0)
  */
class NoShrinkingLengthModifier(startLength: StackLength = StackLength.any.expanding) extends StackLengthModifier
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
