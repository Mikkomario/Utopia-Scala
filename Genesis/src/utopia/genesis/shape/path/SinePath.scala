package utopia.genesis.shape.path

/**
  * A simple path that follows a sine curve from 0 to 2pi radians
  * @author Mikko Hilpinen
  * @since 18.4.2021, v2.4.1
  */
object SinePath extends Path[Double]
{
	override def start = 0.0
	
	override def end = 0.0
	
	override def apply(progress: Double) = math.sin(progress * 2 * math.Pi)
}
