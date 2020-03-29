package utopia.genesis.image.transform

/**
  * This transformation inverts image colors
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  */
object Invert extends RGBTransform
{
	override def apply(originalRatio: Double) = 1 - originalRatio
}
