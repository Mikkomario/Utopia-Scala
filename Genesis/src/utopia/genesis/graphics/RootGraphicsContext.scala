package utopia.genesis.graphics

import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape3d.Matrix3D

import scala.concurrent.ExecutionContext

object RootGraphicsContext
{
	/**
	  * Creates a new root level graphics context instance
	  * @param openGraphics A function for opening a new temporary graphics instance
	  * @param exc Implicit execution context
	  * @return A new graphics context instance
	  */
	def apply(openGraphics: => ClosingGraphics)(implicit exc: ExecutionContext) = new RootGraphicsContext(openGraphics)
}

/**
  * The highest available graphics context object that holds a reference to the underlying graphics object
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// TODO: Remove this class
class RootGraphicsContext(newGraphics: => ClosingGraphics) extends GraphicsContext
{
	// ATTRIBUTES   -----------------------------
	
	private val graphicsPointer = ResettableLazy(newGraphics)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def repr = this
	
	override def transformation = Matrix3D.identity
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedGraphicsContext(transformation, this.transformation, _openGraphics -> None)
	
	override def transformedWith(transformation: Matrix2D): DerivedGraphicsContext =
		transformedWith(transformation.to3D)
	
	override def closeCurrent() = graphicsPointer.current.foreach { _.close() }
	
	
	// OTHER    ---------------------------------
	
	private def _openGraphics = {
		graphicsPointer.filter { _.isOpen }
		graphicsPointer.value
	}
}
