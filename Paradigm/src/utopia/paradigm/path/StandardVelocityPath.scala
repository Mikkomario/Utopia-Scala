package utopia.paradigm.path

import utopia.flow.operator.{HasLength, Reversible}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.combine.Combinable
import utopia.paradigm.path.Path.PathWithDistance

/**
  * This path works exactly like a the original path, except that the "velocity" within the curve is standardized
  * @author Mikko Hilpinen
  * @since Genesis 24.6.2019, v2.1+
  */
case class StandardVelocityPath[P <: Combinable[P, P] with Reversible[P] with HasLength]
(private val original: PathWithDistance[P], sequenceAmount: Int)
	extends Path[P] with HasLength
{
	// ATTRIBUTES	-------------------
	
	// TODO: Could optimize this by combining similar values into a single larger range
	private val sequenceLengths = (0 to sequenceAmount).map { i => original(i.toDouble / sequenceAmount) }.paired.map {
		p => (p.second - p.first).length }
	
	// IMPLEMENTED	-------------------
	
	override def start = original.start
	
	override def end = original.end
	
	override def length = original.length
	
	override def apply(t: Double) =
	{
		// Handles cases where t is out of bounds
		if (t <= 0 || t >= 1 || sequenceAmount <= 1)
			original(t)
		else
		{
			// Finds the sequence that contains the target value
			val targetPosition = t * length
			var traversed = 0.0
			
			sequenceLengths.zipWithIndex.findMap
			{
				case (partLength, index) =>
					
					if (traversed + partLength >= targetPosition)
					{
						val tWithinSequence = (targetPosition - traversed) / partLength
						val tPerSequence = 1.0 / sequenceAmount
						val trueT = tPerSequence * (index + tWithinSequence)
						
						Some(original(trueT))
					}
					else
					{
						traversed += partLength
						None
					}
					
			}.getOrElse(original(t))
		}
	}
}
