package utopia.reflection.shape.stack

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Direction2D, Insets, InsetsFactory, InsetsLike}
import utopia.reflection.shape.Alignment

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
	with StackInsetsConvertible
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
	
	/**
	  * @return A copy of these insets where no side has a maximum limit and all sides expand easily
	  */
	def expanding = map { _.expanding.noMax }
	/**
	  * @return A copy of these insets where horizontal sides have no maximum limit and expand easily
	  */
	def expandingHorizontally = expandingAlong(X)
	/**
	  * @return A copy of these insets where vertical sides have no maximum limit and expand easily
	  */
	def expandingVertically = expandingAlong(Y)
	/**
	  * @return A copy of these insets where the right side has no maximum limit and expands easily
	  */
	def expandingToRight = expandingTowards(Direction2D.Right)
	/**
	  * @return A copy of these insets where the left side has no maximum limit and expands easily
	  */
	def expandingToLeft = expandingTowards(Direction2D.Left)
	/**
	  * @return A copy of these insets where the top has no maximum limit and expands easily
	  */
	def expandingToTop = expandingTowards(Direction2D.Up)
	/**
	  * @return A copy of these insets where the bottom has no maximum limit and expands easily
	  */
	def expandingToBottom = expandingTowards(Direction2D.Down)
	
	
	// IMPLEMENTED	-----------------------
	
	@deprecated("There's no need to call this method since 'this' already does this", "v2")
	override def toInsets = this
	override protected def makeCopy(newAmounts: Map[Direction2D, StackLength]) = StackInsets(newAmounts)
	override protected def makeZero  = StackLength.fixedZero
	override protected def combine(first: StackLength, second: StackLength)  = first + second
	override protected def multiply(a: StackLength, multiplier: Double)  = a * multiplier
	override protected def make2D(horizontal: StackLength, vertical: StackLength)  = StackSize(horizontal, vertical)
	
	override def repr = this
	
	
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
	  * @param other A set of insets
	  * @return A copy of these insets which have been increased by the other set of insets
	  */
	def +(other: Insets) = makeCopy(
		(amounts.keySet ++ other.amounts.keySet).map { dir => dir -> (apply(dir) + other(dir)) }.toMap)
	/**
	  * @param amount Length decrease affecting each side of these insets
	  * @return A copy of these insets with each side decreased
	  */
	def -(amount: Double) = if (amount == 0) this else map { _ - amount }
	/**
	  * @param other A set of insets
	  * @return A copy of these insets with the specified insets subtracted on all sides
	  */
	def -(other: Insets) = this + (-other)
	
	/**
	  * Converts these stack insets to normal insets
	  * @param f A mapping function
	  * @return A set of insets with mapped side values
	  */
	def mapToInsets(f: StackLength => Double) = Insets(amounts.map { case (d, l) => d -> f(l) })
	
	/**
	  * @param direction Target direction
	  * @return A copy of these insets that expand to the specified direction, with no maximum value defined either.
	  */
	def expandingTowards(direction: Direction2D) = mapSide(direction) { _.expanding.noMax }
	/**
	  * @param axis Target axis
	  * @return A copy of these insets that expand along the specified axis, with no maximum value defined either.
	  */
	def expandingAlong(axis: Axis2D) = mapAxis(axis) { _.expanding.noMax }
	
	/**
	  * @param axis Target axis
	  * @param alignment Contextual alignment
	  * @return A copy of these insets which expand to the direction opposite to the aligned side. For example, if
	  *         axis = X and alignment is Right, expands to Left. If alignment is center, expands to both directions.
	  */
	def expandingAccordingTo(axis: Axis2D, alignment: Alignment) = alignment.directionAlong(axis) match
	{
		case Some(preservedDirection) => expandingTowards(axis.toDirection(preservedDirection.opposite))
		case None => expandingAlong(axis)
	}
	/**
	  * @param alignment Contextual alignment
	  * @return A copy of these insets that expand to the opposite direction of the aligned side. For example, if
	  *         alignment is Right, expands to Left and if alignment is Center, expands to Left and Right.
	  */
	def expandingHorizontallyAccordingTo(alignment: Alignment) = expandingAccordingTo(X, alignment)
	/**
	  * @param alignment Contextual alignment
	  * @return A copy of these insets that expand to the direction opposite to the aligned side. For example,
	  *         if alignment is Top, expands to Bottom and if alignment is Center, expands to Top and Bottom
	  */
	def expandingVerticallyAccordingTo(alignment: Alignment) = expandingAccordingTo(Y, alignment)
}