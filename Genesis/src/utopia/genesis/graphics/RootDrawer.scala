package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.{Bounds, Matrix2D}
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * The highest available drawer instance that holds a graphics object
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
class RootDrawer(override protected val graphics: ClosingGraphics) extends Drawer2 with AutoCloseable
{
	// ATTRIBUTES   --------------------------
	
	override lazy val clipBounds = Bounds.fromAwt(graphics.getClipBounds)
	
	
	// IMPLEMENTED  --------------------------
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedDrawer((graphics, Some(transformation), Vector()), clipBounds -> transformation.inverse)
	
	override def transformedWith(transformation: Matrix2D): DerivedDrawer = transformedWith(transformation.to3D)
}
