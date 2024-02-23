package utopia.genesis.handling.drawing

import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Drawer
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * A wrapper for a Drawable instance that applies an additional transformation layer
  * @author Mikko Hilpinen
  * @since 21/02/2024, v4.0
  */
class Transformer(override protected val wrapped: Drawable2, transformPointer: Changing[Matrix3D])
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
	
	override protected val mouseHandler = new TransformedMouseHandler()
	
	
	// INITIAL CODE -----------------------
	
	// Relays the repaint requests forward, but with transformed sub-regions
	wrapped.addRepaintListener { (_, region, prio) =>
		val transformedRegion = region.flatMap { r =>
			inverseTransformPointer.value.map { t => (r * t).bounds - drawBounds.position }
		}
		repaint(transformedRegion, prio)
	}
	
	
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
