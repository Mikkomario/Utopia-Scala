package utopia.genesis.image.transform

/**
  * This color transformation limits the number of color values per channel
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  */
case class Threshold(colorAmount: Int) extends RGBTransform
{
	override def apply(originalRatio: Double) = (originalRatio * colorAmount).round / colorAmount.toDouble
}
