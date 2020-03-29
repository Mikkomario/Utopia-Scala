package utopia.genesis.shape.path

/**
  * This simply wraps a path and maps the results. Still has the same length as the original, however.
  * @author Mikko Hilpinen
  * @since 12.8.2019, v2.1+
  */
case class MappedPath[A, +B](original: Path[A], map: A => B) extends Path[B]
{
	override def start = map(original.start)
	
	override def end = map(original.end)
	
	override def length = original.length
	
	override def apply(t: Double) = map(original(t))
}
