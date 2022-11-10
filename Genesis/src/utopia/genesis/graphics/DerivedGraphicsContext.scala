package utopia.genesis.graphics

import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape3d.Matrix3D

/**
  * A graphics context instance that relies on a parent instance and applies a transformation over it
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// ParentState contains a graphics instance + possible transformation
// TODO: Remove this class (Replaced with GraphicsContext2)
class DerivedGraphicsContext(lastTransformation: Matrix3D, parentTransformation: Matrix3D,
                             parentState: => (ClosingGraphics, Option[Matrix3D]))
	extends GraphicsContext
{
	// ATTRIBUTES   ----------------------------------
	
	private lazy val _transformation = parentTransformation(lastTransformation)
	// New graphics instances are only created when necessary
	private val graphicsPointer = ResettableLazy {
		// Starts from the available parent state
		val (parentGraphics, baseTransformation) = parentState
		// Applies the correct transformation
		val transformation = baseTransformation match
		{
			case Some(base) => base(lastTransformation)
			case None => lastTransformation
		}
		val newGraphics = parentGraphics.createChild()
		newGraphics.transform(transformation)
		newGraphics
	}
	
	
	// IMPLEMENTED  ----------------------------------
	
	override def repr = this
	
	override def transformation = _transformation
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedGraphicsContext(transformation, _transformation, graphicsPointer.current.filter { _.isOpen } match
		{
			case Some(graphics) => graphics -> None
			case None =>
				val (parentGraphics, baseTransformation) = parentState
				val newBaseTransformation = baseTransformation match
				{
					case Some(base) => base(lastTransformation)
					case None => lastTransformation
				}
				parentGraphics -> Some(newBaseTransformation)
		})
	
	override def transformedWith(transformation: Matrix2D): DerivedGraphicsContext =
		transformedWith(transformation.to3D)
	
	override def closeCurrent() = graphicsPointer.current.foreach { _.close() }
	
	
	// OTHER    -------------------------------------
	
	private def _openGraphics =
	{
		val default = graphicsPointer.value
		if (default.isOpen)
			default
		else
		{
			graphicsPointer.reset()
			graphicsPointer.value
		}
	}
}
