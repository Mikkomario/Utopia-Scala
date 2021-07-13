package utopia.genesis.graphics

import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.genesis.shape.shape2D.Matrix2D
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * A graphics context instance that relies on a parent instance and applies a transformation over it
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// ParentState contains a graphics instance + possible transformation
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
