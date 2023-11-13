package utopia.flow.operator.combine

import utopia.flow.operator.Reversible

/**
  * A common trait for items which can be scaled linearly (using Doubles)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait LinearScalable[+Repr] extends Any with Scalable[Double, Repr] with Reversible[Repr]
{
	// IMPLEMENTED  --------------------------
	
	override def unary_- = this * -1
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param div A divider
	  * @return A divided copy of this instance
	  */
	def /(div: Double) = this * (1.0 / div)
}
