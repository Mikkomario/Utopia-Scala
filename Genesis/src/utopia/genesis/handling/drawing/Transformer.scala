package utopia.genesis.handling.drawing

import utopia.flow.event.listener.ChangeListener
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Drawer
import utopia.genesis.graphics.Priority.Low
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * A wrapper for a Drawable instance that applies an additional transformation layer
  * @author Mikko Hilpinen
  * @since 21/02/2024, v4.0
  */
class Transformer(override protected val wrapped: Drawable, transformPointer: Changing[Matrix3D])(implicit log: Logger)
	extends TransformingDrawableWrapper
{
	// ATTRIBUTES   -----------------------
	
	private val inverseTransformPointer = transformPointer.map { _.inverse }
	private val transformedDrawBoundsPointer = wrapped.drawBoundsPointer
		.mergeWithWhile(inverseTransformPointer, wrapped.handleCondition) { (b, t) =>
			t match {
				case Some(t) => b * t
				case None => b.withSize(Size.zero)
			}
		}
	override val drawBoundsPointer: Changing[Bounds] = transformedDrawBoundsPointer.map { _.bounds }
	
	private var _repaintListeners = Seq[RepaintListener]()
	private val repaintOnChangeListener = ChangeListener.onAnyChange { repaint(priority = Low) }
	
	override protected val mouseHandler = new TransformedMouseHandler()
	
	
	// INITIAL CODE -----------------------
	
	// Whenever the wrapped draw bounds change, repaints
	wrapped.drawBoundsPointer.addListener(repaintOnChangeListener)
	// Also repaints when the transformation is altered
	transformPointer.addListener(repaintOnChangeListener)
	
	
	// IMPLEMENTED  -----------------------
	
	override def opaque: Boolean = false
	
	override def repaintListeners = _repaintListeners
	override protected def repaintListeners_=(listeners: Seq[RepaintListener]): Unit = _repaintListeners = listeners
	
	override def toView[V <: DoubleVectorLike[V]](p: V): V = p * transformPointer.value
	override def view[V <: DoubleVectorLike[V]](viewPoint: V): V = inverseTransformPointer.value match {
		case Some(t) => viewPoint * t
		case None => viewPoint.zero
	}
	override protected def viewSubRegion(region: Bounds): Bounds = inverseTransformPointer.value match {
		case Some(t) => (region * t).bounds - drawBounds.position
		case None => region.withSize(Size.zero)
	}
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = {
		val transformedDrawer = {
			// Case: No translation is applied => Draws the wrapped item in its default state using a transformed drawer
			if (bounds.position ~== drawBounds.position)
				drawer * transformPointer.value
			// Case: Translation is applied => Translates the drawer after transformation has been applied
			else
				(drawer * transformPointer.value).translated(bounds.position - drawBounds.position)
		}
		wrapped.draw(transformedDrawer, wrapped.drawBounds)
	}
}
