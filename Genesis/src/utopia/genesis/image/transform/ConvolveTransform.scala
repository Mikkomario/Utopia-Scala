package utopia.genesis.image.transform

import java.awt.image.{ConvolveOp, Kernel}

import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Size

/**
  * This image transform uses java's ConvolveOp
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  */
class ConvolveTransform(kernel: Array[Float], kernelSize: Size) extends ImageTransform
{
	// ATTRIBUTES	-----------------
	
	private val op = new ConvolveOp(new Kernel(kernelSize.width.toInt, kernelSize.height.toInt, kernel),
		ConvolveOp.EDGE_NO_OP, null)
	
	
	// IMPLEMENTED	-----------------
	
	override def apply(source: Image) = source.filterWith(op)
}
