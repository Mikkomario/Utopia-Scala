package utopia.genesis.shape.path

/**
  * This path doesn't move anywhere from a single point
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1+
  */
case class EmptyPath[P](point: P) extends Path[P]
{
	override def start = point
	
	override def end = point
	
	override def length = 0.0
	
	override def apply(t: Double) = point
}
