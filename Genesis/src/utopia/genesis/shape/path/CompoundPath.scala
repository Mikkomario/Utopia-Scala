package utopia.genesis.shape.path

import utopia.flow.util.CollectionExtensions._

object CompoundPath
{
	/**
	  * Combines multiple paths
	  * @param first The first path
	  * @param second The second path
	  * @param more more paths
	  * @tparam P The type of paths
	  * @return A new compound path
	  */
	def apply[P](first: Path[P], second: Path[P], more: Path[P]*): CompoundPath[P] = CompoundPath(Vector(first, second) ++ more)
}

/**
  * Compound paths consist of multiple smaller path segments
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1+
  */
case class CompoundPath[+P](parts: Vector[Path[P]]) extends Path[P]
{
	// INITIAL CODE	-------------------
	
	if (parts.isEmpty)
		throw new IllegalArgumentException("Compund path must be initialized with at least a single part")
	
	
	// IMPLEMENTED	-------------------
	
	lazy val length = parts.foldLeft(0.0) { _ + _.length }
	
	override def start = parts.head.start
	
	override def end = parts.last.end
	
	override def apply(t: Double) =
	{
		// Handles cases where t is out of bounds
		if (t <= 0)
			parts.head(t * (1 / (parts.head.length / length)))
		else if (t >= 1)
		{
			val lastLength = parts.last.length
			val startT = (length - lastLength) / length
			val tInPart = (t - startT) * (1 / (lastLength / length))
			parts.last(tInPart)
		}
		else
		{
			// Traverses paths until a suitable is found
			val targetPosition = t * length
			var traversed = 0.0
			parts.findMap
			{
				part =>
					val length = part.length
					if (traversed + length >= targetPosition)
					{
						val positionInPart = targetPosition - traversed
						val tInsidePart = positionInPart / length
						Some(part(tInsidePart))
					}
					else
					{
						traversed += length
						None
					}
			}.get
		}
	}
}
