package utopia.genesis.handling.drawing

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.{DrawOrder, Drawer}
import utopia.paradigm.enumeration.FillAreaLogic.{Fit, ScalePreservingShape}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Modifies where and how large another Drawable item is drawn
  * @author Mikko Hilpinen
  * @since 11/02/2024, v4.0
  */
class Repositioner(wrapped: Drawable2, targetPointer: Either[(Changing[Point], Changing[Size]), Changing[Bounds]],
                   resizeLogic: ScalePreservingShape = Fit)
	extends Drawable2
{
	// ATTRIBUTES   --------------------
	
	private val (targetPositionPointer, targetSizePointer) =
		targetPointer.leftOrMap { b => b.strongMap { _.position } -> b.strongMap { _.size } }
	private val originalSizePointer = wrapped.drawBoundsPointer.strongMap { _.size }
	
	private val relativeBoundsPointer = originalSizePointer.mergeWith(targetSizePointer)(resizeLogic.apply)
	override val drawBoundsPointer = relativeBoundsPointer.mergeWith(targetPositionPointer) { _ + _ }
	
	private val scalingPointer = targetSizePointer.mergeWith(originalSizePointer) { _.x / _.x }
	
	private var _repaintListeners = Vector.empty[RepaintListener]
	
	
	// INITIAL CODE --------------------

	// Whenever the wrapped item requests a repaint, modifies the repaint call to match the new position
	wrapped.addRepaintListener { (_, region, priority) => repaint(region.map { _ * scalingPointer.value }, priority) }
	
	
	// IMPLEMENTED  --------------------
	
	override def handleCondition: FlagLike = wrapped.handleCondition
	
	override def drawOrder: DrawOrder = wrapped.drawOrder
	override def opaque: Boolean = wrapped.opaque
	override protected def repaintListeners: Iterable[RepaintListener] = _repaintListeners
	
	override def addRepaintListener(listener: RepaintListener): Unit = {
		if (!_repaintListeners.contains(listener))
			_repaintListeners :+= listener
	}
	override def removeRepaintListener(listener: RepaintListener): Unit =
		_repaintListeners = _repaintListeners.filterNot { _ == listener }
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = {
		// Modifies the drawer so that (0,0) lies at the targeted draw position, with correct scaling applied
		val modifiedDrawer = drawer
			.translated(bounds.position + relativeBoundsPointer.value.position)
			.scaled(scalingPointer.value)
		wrapped.draw(modifiedDrawer, Bounds(Point.origin, wrapped.drawBounds.size))
	}
}
