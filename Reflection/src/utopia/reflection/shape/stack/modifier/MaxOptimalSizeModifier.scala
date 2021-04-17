package utopia.reflection.shape.stack.modifier

import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Size
import utopia.reflection.shape.stack.StackSize

/**
  * A stack size modifier which places a maximum limit on the optimal stack size
  * @author Mikko Hilpinen
  * @since 9.12.2020, v2
  */
case class MaxOptimalSizeModifier(max: Size) extends StackSizeModifier
{
	private val modifiers = Axis2D.values.map { axis => axis -> MaxOptimalLengthModifier(max.along(axis)) }.toMap
	
	override def apply(size: StackSize) = size.map { (axis, length) => modifiers(axis)(length) }
}
