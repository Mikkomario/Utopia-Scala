package utopia.reflection.shape

import utopia.genesis.shape.shape2D.Direction2D

object StackInsets extends InsetsFactory[StackLength, StackSize, StackInsets, StackInsets]
{
	// ATTRIBUTES	-----------------------
	
	/**
	  * A set of insets where each side has "any" length (0 or more, preferring 0)
	  */
	val any = symmetric(StackLength.any)
	
	/**
	  * A set of insets where each side is fixed to 0
	  */
	val zero = symmetric(StackLength.fixedZero)
	
	
	// OTHER	---------------------------
	
	/**
	  * Creates a symmetric set of stack insets
	  * @param margins Combined stack insets on each side
	  * @return A symmetric set of insets where the total value is equal to the provided size
	  */
	def symmetric(margins: StackSize): StackInsets = symmetric(margins.width / 2, margins.height / 2)
}

/**
  * A set of insets made of stack lengths
  * @author Mikko Hilpinen
  * @since 2.2.2020, v1
  */
case class StackInsets(amounts: Map[Direction2D, StackLength]) extends InsetsLike[StackLength, StackSize, StackInsets]
{
	// ATTRIBUTES	-----------------------
	
	/**
	  * The optimal insets within these insets
	  */
	lazy val optimal = mapToInsets { _.optimal }
	
	/**
	  * The minimum insets within these insets
	  */
	lazy val min = mapToInsets { _.min }
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @return A copy of these insets where each side is marked with low priority
	  */
	def withLowPriority = StackInsets(Direction2D.values.map { d => d -> apply(d).withLowPriority }.toMap)
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def makeCopy(newAmounts: Map[Direction2D, StackLength]) = StackInsets(newAmounts)
	override protected def makeZero  = StackLength.fixedZero
	override protected def combine(first: StackLength, second: StackLength)  = first + second
	override protected def multiply(a: StackLength, multiplier: Double)  = a * multiplier
	override protected def make2D(horizontal: StackLength, vertical: StackLength)  = StackSize(horizontal, vertical)
	
	
	// OTHER	---------------------------
	
	/**
	  * @param amount Length increase affecting each side of these insets
	  * @return A copy of these insets with each side increased
	  */
	def +(amount: Double) = if (amount == 0) this else map { _ + amount }
	/**
	  * @param other Length increase affecting each side of these insets
	  * @return A copy of these insets with each side increased
	  */
	def +(other: StackLength) = map { _ + other }
	/**
	  * @param amount Length decrease affecting each side of these insets
	  * @return A copy of these insets with each side decreased
	  */
	def -(amount: Double) = if (amount == 0) this else map { _ - amount }
	
	/**
	  * Converts these stack insets to normal insets
	  * @param f A mapping function
	  * @return A set of insets with mapped side values
	  */
	def mapToInsets(f: StackLength => Double) = Insets(amounts.map { case (d, l) => d -> f(l) })
}