package utopia.paradigm.path

import utopia.flow.operator.HasLength
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.paradigm.path.Path.PathWithDistance

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
	def apply[P](first: PathWithDistance[P], second: PathWithDistance[P], more: PathWithDistance[P]*): CompoundPath[P] =
		CompoundPath(Pair(first, second) ++ more)
}

/**
  * Compound paths consist of multiple smaller path segments
  * @author Mikko Hilpinen
  * @since Genesis 20.6.2019, v2.1+
  */
case class CompoundPath[+P](parts: Seq[PathWithDistance[P]]) extends Path[P] with HasLength
{
	// ATTRIBUTES   -------------------
	
	override lazy val length = parts.foldLeft(0.0) { _ + _.length }
	
	
	// INITIAL CODE	-------------------
	
	if (parts.isEmpty)
		throw new IllegalArgumentException("Compund path must be initialized with at least a single part")
	
	
	// IMPLEMENTED	-------------------
	
	override def start = parts.head.start
	
	override def end = parts.last.end
	
	override def apply(t: Double) = {
		// Handles cases where t is out of bounds
		if (t <= 0)
			parts.head(t * (1 / (parts.head.length / length)))
		else if (t >= 1) {
			val lastLength = parts.last.length
			val startT = (length - lastLength) / length
			val tInPart = (t - startT) * (1 / (lastLength / length))
			parts.last(tInPart)
		}
		else {
			// Traverses paths until a suitable is found
			val targetPosition = t * length
			var traversed = 0.0
			parts.findMap { part =>
				val length = part.length
				if (traversed + length >= targetPosition) {
					val positionInPart = targetPosition - traversed
					val tInsidePart = positionInPart / length
					Some(part(tInsidePart))
				}
				else {
					traversed += length
					None
				}
			}.get
		}
	}
}
