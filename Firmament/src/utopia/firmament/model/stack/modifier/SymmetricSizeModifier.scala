package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.{StackLength, StackSize}

/**
  * Makes all stack sizes as symmetric as possible
  * @author Mikko Hilpinen
  * @since 24.4.2020, Reflection v1.2
  */
object SymmetricSizeModifier extends StackSizeModifier
{
	override def apply(size: StackSize) = {
		val sides = size.xyPair
		val min = sides.map {_.min}.max
		val optimal = sides.map {_.optimal}.max
		val max = sides.flatMap {_.max}.minOption
		
		StackLength(min, optimal, max, sides.map {_.priority}.reduce {_ max _}).square
	}
}
