package utopia.paradigm.path

/**
  * This simply wraps a path and maps the results.
  * @author Mikko Hilpinen
  * @since Genesis 12.8.2019, v2.1+
  */
case class MappedPath[+A, +B](original: Path[A])(map: A => B) extends Path[B]
{
	override def start = map(original.start)
	
	override def end = map(original.end)
	
	override def apply(t: Double) = map(original(t))
}
