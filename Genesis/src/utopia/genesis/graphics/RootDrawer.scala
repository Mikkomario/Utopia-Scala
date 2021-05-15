package utopia.genesis.graphics
import utopia.genesis.shape.shape2D.{Bounds, Matrix2D}
import utopia.genesis.shape.shape3D.Matrix3D

import scala.concurrent.ExecutionContext

/**
  * The highest available drawer instance that holds a graphics object
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
class RootDrawer(override protected val writeContext: WriteableGraphicsContext)(implicit exc: ExecutionContext)
	extends Drawer2 with AutoCloseable
{
	// ATTRIBUTES   --------------------------
	
	override lazy val clipBounds = Bounds.fromAwt(graphics.getClipBounds)
	
	
	// IMPLEMENTED  --------------------------
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedDrawer(writeContext.transformedWith(transformation), clipBounds -> transformation.inverse)
	
	override def transformedWith(transformation: Matrix2D): DerivedDrawer = transformedWith(transformation.to3D)
}
