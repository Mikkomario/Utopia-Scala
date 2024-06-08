package utopia.terra.model.map

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
 * Common trait for points that bind a world position into a 2D map location
 * @author Mikko Hilpinen
 * @since 31.8.2023, v1.0
 * @tparam V Type of vector representation used
 * @tparam Repr Implementing type of this trait
 */
trait MapPointLike[V, +Repr] extends Combinable[MapPoint[V], Repr] with LinearScalable[Repr] with EqualsBy
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return A vector representation of this world point
	 */
	def vector: V
	/**
	 * @return Location in the 2D map that corresponds to this point.
	 *         Relative to the map origin.
	 */
	def mapLocation: Point
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def equalsProperties: Seq[Any] = Pair(vector, mapLocation)
}
