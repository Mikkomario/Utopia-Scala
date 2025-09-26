package utopia.firmament.test

import utopia.firmament.model.stack.{StackInsets, StackSize}
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
 * @author Mikko Hilpinen
 * @since 25.09.2025, v1.6
 */
object StackLengthTest extends App
{
	private val size = Size(56, 40)
	assert(StackSize.fixed(size) + StackInsets.zero == StackSize.fixed(size))
}
