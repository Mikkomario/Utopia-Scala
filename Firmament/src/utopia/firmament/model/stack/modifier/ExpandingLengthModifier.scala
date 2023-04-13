package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackLength

/**
  * This stack length modifier makes all lengths easily expandable (provided they have room to expand)
  * @author Mikko Hilpinen
  * @since 30.8.2020, Reflection v1.2.1
  */
object ExpandingLengthModifier extends StackLengthModifier
{
	override def apply(length: StackLength) = {
		// Creates an expanding copy, unless already maxed out
		if (length.max.forall {_ > length.optimal})
			length.expanding
		else
			length
	}
}
