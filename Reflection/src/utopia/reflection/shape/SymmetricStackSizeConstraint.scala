package utopia.reflection.shape

import scala.math.Ordering.Double.TotalOrdering

/**
 * Makes all stack sizes as symmetric as possible
 * @author Mikko Hilpinen
 * @since 24.4.2020, v1.2
 */
object SymmetricStackSizeConstraint extends StackSizeModifier
{
	override def apply(size: StackSize) =
	{
		val sides = size.components
		val min = sides.map { _.min }.max
		val optimal = sides.map { _.optimal }.max
		val max = sides.flatMap { _.max }.minOption
		
		StackLength(min, optimal, max, sides.map { _.priority }.reduce { _ max _ }).square
	}
}
