package utopia.genesis.image.transform

import java.awt.image.{ConvolveOp, Kernel}

import utopia.genesis.image.{Image, MutableImage}
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * This image transform uses java's ConvolveOp
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  */
class ConvolveTransform(kernel: Array[Float], kernelSize: Size) extends ImageTransform
{
	// ATTRIBUTES	-----------------
	
	/**
	  * The operation applied to a buffered (AWT) image in this transform
	  */
	val op = new ConvolveOp(new Kernel(kernelSize.width.toInt, kernelSize.height.toInt, kernel),
		ConvolveOp.EDGE_NO_OP, null)
	
	
	// IMPLEMENTED	-----------------
	
	override def apply(source: Image) = source.filterWith(op)
	
	override def apply(target: MutableImage) = target.filterWith(op)
}
