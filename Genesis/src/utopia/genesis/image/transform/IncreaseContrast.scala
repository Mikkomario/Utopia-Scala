package utopia.genesis.image.transform

import utopia.flow.collection.CollectionExtensions._

object IncreaseContrast extends IncreaseContrast(1)

/**
  * This transformation increases color contrast
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  */
sealed class IncreaseContrast private(val iterations: Double) extends RgbTransform
{
	// IMPLEMENTED	------------------
	
	override def apply(originalRatio: Double) =
		Iterator.iterate(originalRatio)(applyOnce).take((iterations + 1).ceil.toInt).last
	
	// OPERATORS	-------------------
	
	/**
	  * Adjusts the number of iterations for this operation
	  * @param multiplier A multiplier for the number of iterations
	  * @return An adjusted version of this transform
	  */
	def *(multiplier: Double) = new IncreaseContrast(iterations * multiplier)
	
	/**
	  * Adjusts the number of iterations for this operation
	  * @param div A divider for the number of iterations
	  * @return An adjusted version of this transform
	  */
	def /(div: Double) = new IncreaseContrast(iterations / div)

	
	// OTHER	-----------------------
	
	private def applyOnce(x: Double) = 3 * Math.pow(x, 2) - 2 * Math.pow(x, 2)
}
