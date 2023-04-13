package utopia.firmament.model.stack.modifier

import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.Size
import utopia.firmament.model.stack.StackSize

/**
  * A stack size modifier which places a maximum limit on the optimal stack size
  * @author Mikko Hilpinen
  * @since 9.12.2020, Reflection v2
  */
case class MaxOptimalSizeModifier(max: Size) extends StackSizeModifier
{
	private val modifiers = Axis2D.values.map { axis => axis -> MaxOptimalLengthModifier(max(axis)) }.toMap
	
	override def apply(size: StackSize) = size.map { (axis, length) => modifiers(axis)(length) }
}
