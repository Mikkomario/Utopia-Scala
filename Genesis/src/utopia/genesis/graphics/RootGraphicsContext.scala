package utopia.genesis.graphics

import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.genesis.shape.shape2D.Matrix2D
import utopia.genesis.shape.shape3D.Matrix3D

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
class RootGraphicsContext(newGraphics: => ClosingGraphics) extends GraphicsContext
{
	// ATTRIBUTES   -----------------------------
	
	private val graphicsPointer = ResettableLazy(newGraphics)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def transformation = Matrix3D.identity
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedGraphicsContext(transformation, this.transformation, _openGraphics -> None)
	
	override def transformedWith(transformation: Matrix2D): DerivedGraphicsContext =
		transformedWith(transformation.to3D)
	
	override def closeCurrent() = graphicsPointer.current.foreach { _.close() }
	
	
	// OTHER    ---------------------------------
	
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
