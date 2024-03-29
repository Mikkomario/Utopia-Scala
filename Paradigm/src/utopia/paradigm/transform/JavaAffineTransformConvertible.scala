package utopia.paradigm.transform

import java.awt.geom.AffineTransform

/**
  * A common trait for transformation representations that can be converted to java affine transformations
  * @author Mikko Hilpinen
  * @since Genesis 26.12.2020, v2.4
  */
trait JavaAffineTransformConvertible
{
	/**
	  * @return A java geom AffineTransform based on this transformation
	  */
	def toJavaAffineTransform: AffineTransform
}
