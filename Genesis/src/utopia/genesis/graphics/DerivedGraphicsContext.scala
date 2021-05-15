package utopia.genesis.graphics
import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.genesis.shape.shape2D.Matrix2D
import utopia.genesis.shape.shape3D.Matrix3D

import scala.concurrent.ExecutionContext

/**
  * A graphics context instance that relies on a parent instance and applies a transformation over it
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// ParentState contains a graphics instance + possible transformation
class DerivedGraphicsContext(lastTransformation: Matrix3D, parentTransformation: Matrix3D,
                             parentState: => (ClosingGraphics, Option[Matrix3D]))
                            (implicit exc: ExecutionContext)
	extends WriteableGraphicsContext
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
		// Makes sure this graphics pointer is invalidated once this new graphics instance closes
		newGraphics.closeFuture.foreach { _ => invalidateGraphics() }
		newGraphics
	}
	
	
	// IMPLEMENTED  ----------------------------------
	
	override def openGraphics = graphicsPointer.value
	
	override def transformation = _transformation
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedGraphicsContext(transformation, _transformation, graphicsPointer.current match
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
	
	private def invalidateGraphics(): Unit = graphicsPointer.reset()
}
