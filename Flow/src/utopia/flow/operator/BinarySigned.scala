package utopia.flow.operator

/**
  * Common trait for items that can be positive or negative, but never zero
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
trait BinarySigned[+Repr] extends Signed[Repr]
{
	// IMPLEMENTED  --------------------
	
	override def isNegative = !isPositive
}
