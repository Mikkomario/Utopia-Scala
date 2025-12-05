package utopia.access.model.header

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.view.immutable.View

/**
 * Common trait for individual HTTP header representations
 * @tparam A Type of this header's parsed value
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait HeaderLike[+A, +V <: HeaderValue[A]] extends View[V] with EqualsBy
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The name of this header
	 */
	def name: String
	
	
	// COMPUTED ------------------------
	
	def toConstant = Constant(name, value.toValue)
	
	
	// IMPLEMENTED  --------------------
	
	override def toString: String = s"$name: $value"
	
	override protected def equalsProperties: IterableOnce[Any] = Pair(name, value)
}
