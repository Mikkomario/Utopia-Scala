package utopia.firmament.test

import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.enumeration.Alignment.Top
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
 * @author Mikko Hilpinen
 * @since 25.09.2025, v1.6
 */
object StackAlignmentTest extends App
{
	ParadigmDataType.setup()
	
	// alignment.positionWithInsets(layer.stackSize.optimal, targetArea, optimalMargin.any)
	private val targetArea = Bounds(Point(10, 10), Size(10, 10))
	private val area = Size(4, 4)
	private val margin = 1.any
	
	private val pTop = Top.positionWithInsets(area, targetArea, margin)
	assert(pTop == Bounds(Point(13, 11), Size(4, 4)), pTop)
}
