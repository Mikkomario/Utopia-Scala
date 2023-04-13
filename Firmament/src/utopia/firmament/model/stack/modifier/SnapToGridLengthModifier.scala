package utopia.firmament.model.stack.modifier

import utopia.flow.collection.immutable.Pair
import utopia.firmament.model.stack.StackLength

/**
  * A stack length modifier that attempts to keep the length divisible by a certain value.
  * Usually this is to avoid making small changes or twitches in component size.
  * @author Mikko Hilpinen
  * @since 11.10.2022, Reflection v2.0
  */
case class SnapToGridLengthModifier(interval: Double) extends StackLengthModifier
{
	override def apply(length: StackLength) = {
		// Moves the values to the grid
		val optimalOptions = optionsFrom(length.optimal)
		val minOptions = optionsFrom(length.min)
		val maxOptions = length.max.map(optionsFrom)
		
		// Checks which way the values should be moved. Prefers to enlargen them.
		// Case: Possible to make larger or not possible to make smaller => Makes larger
		if (length.max.forall { _ >= optimalOptions.second } || optimalOptions.first < length.min)
			StackLength(minOptions.second, optimalOptions.second,
				maxOptions.map { m => m.first max optimalOptions.second })
		// Case: Not possible to make larger but possible to make smaller => Makes smaller
		else
			StackLength(minOptions.second min optimalOptions.first, optimalOptions.first, maxOptions.map { _.first })
	}
	
	private def optionsFrom(l: Double) = {
		val smaller = l - l % interval
		Pair(smaller, smaller + interval)
	}
}
