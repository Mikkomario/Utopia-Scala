package utopia.genesis.handling.drawing

import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.template.Handlers
import utopia.genesis.shape.shape2D.MutableTransformable
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.paradigm.transform.AffineTransformation

/**
  * A Drawable wrapper that applies transformations to the drawn instance, also providing a mutable interface for the
  * transformation.
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
class MutableTransformer(item: Drawable2, initialTransform: AffineTransformation = AffineTransformation.identity)
	extends DrawableWrapper with MutableTransformable with CoordinateTransform
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A mutable pointer that contains this item's current (affine) transformation state
	  */
	val transformPointer = EventfulPointer(initialTransform)
	private val transformationPointer = transformPointer.strongMap { _.toMatrix }
	
	private val transformer = new Transformer(item, transformationPointer)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def wrapped: Drawable2 = transformer
	
	override def transformation: AffineTransformation = transformPointer.value
	override def transformation_=(newTransformation: AffineTransformation): Unit =
		transformPointer.value = newTransformation
	
	override def toView[V <: DoubleVectorLike[V]](p: V) = transformer.toView(p)
	override def view[V <: DoubleVectorLike[V]](viewPoint: V) = transformer.view(viewPoint)
	
	override def setupMouseEvents(parentHandlers: Handlers, disableMouseToWrapped: Boolean) =
		transformer.setupMouseEvents(parentHandlers, disableMouseToWrapped)
}
