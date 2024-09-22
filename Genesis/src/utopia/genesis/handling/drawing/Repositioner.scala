package utopia.genesis.handling.drawing

import utopia.flow.util.EitherExtensions._
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Drawer
import utopia.genesis.graphics.Priority.Low
import utopia.paradigm.enumeration.FillAreaLogic.{Fit, ScalePreservingShape}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * Modifies where and how large another Drawable item is drawn
  * @author Mikko Hilpinen
  * @since 11/02/2024, v4.0
  */
class Repositioner(override protected val wrapped: Drawable,
                   targetPointer: Either[(Changing[Point], Changing[Size]), Changing[Bounds]],
                   resizeLogic: ScalePreservingShape = Fit)
	extends TransformingDrawableWrapper
{
	// ATTRIBUTES   --------------------
	
	private val (targetPositionPointer, targetSizePointer) =
		targetPointer.leftOrMap { b => b.strongMap { _.position } -> b.strongMap { _.size } }
	private val originalSizePointer = wrapped.drawBoundsPointer.strongMap { _.size }
	
	private val relativeBoundsPointer = originalSizePointer.mergeWith(targetSizePointer)(resizeLogic.apply)
	override val drawBoundsPointer = relativeBoundsPointer.mergeWith(targetPositionPointer) { _ + _ }
	
	private val scalingPointer = relativeBoundsPointer.mergeWith(originalSizePointer) { _.width / _.width }
	
	override protected val mouseHandler = new TransformedMouseHandler()
	
	private var _repaintListeners = Seq.empty[RepaintListener]
	
	
	// INITIAL CODE --------------------
	
	// Whenever the wrapped item's draw bounds update, repaints, even if the repositioned bounds won't change
	wrapped.drawBoundsPointer.addAnyChangeListener { repaint(priority = Low) }
	
	
	// IMPLEMENTED  --------------------
	
	override def opaque: Boolean = wrapped.opaque
	
	override def repaintListeners = _repaintListeners
	override protected def repaintListeners_=(listeners: Seq[RepaintListener]): Unit = _repaintListeners = listeners
	
	override protected def viewSubRegion(region: Bounds): Bounds = region * scalingPointer.value
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = {
		// Modifies the drawer so that (0,0) lies at the targeted draw position, with correct scaling applied
		val modifiedDrawer = drawer
			.translated(bounds.position + relativeBoundsPointer.value.position)
			.scaled(scalingPointer.value)
		wrapped.draw(modifiedDrawer, Bounds(Point.origin, wrapped.drawBounds.size))
	}
	
	/**
	  * Converts a point in the space relative to this item, to a space relative to the wrapped item.
	  * @param p A point to convert
	  * @tparam V Type of the specified point
	  * @return A matching point in the view space (i.e. the wrapped item's coordinate system)
	  */
	override def toView[V <: DoubleVectorLike[V]](p: V) = {
		// Converts to a point relative to the displayed item draw-bounds
		val relativeToDrawArea = p - targetPositionPointer.value - relativeBoundsPointer.value.position
		// Applies scaling to match relative position to the item draw-bounds
		val scaled = relativeToDrawArea / scalingPointer.value
		// Corrects for the view position
		scaled + wrapped.drawBounds.position
	}
	/**
	  * Converts a point from the view space (i.e. from space relative to the wrapped item),
	  * to space relative to this item.
	  * @param viewPoint A point in the wrapped item's space / view space.
	  * @tparam V Type of the specified point
	  * @return A matching point in this item's space
	  */
	override def view[V <: DoubleVectorLike[V]](viewPoint: V) = {
		// Converts to a point relative to the wrapped item's draw bounds
		val relativeToItemDrawBounds = viewPoint - wrapped.drawBounds.position
		// Applies scaling to match the position in the visual space
		val scaled = relativeToItemDrawBounds * scalingPointer.value
		// Corrects for the display position
		scaled + targetPositionPointer.value
	}
}
