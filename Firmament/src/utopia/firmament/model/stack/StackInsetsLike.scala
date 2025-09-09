package utopia.firmament.model.stack

import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Alignment, Axis2D, Direction2D}
import utopia.paradigm.shape.shape2d.insets.{Insets, ScalableSidesLike}

/**
  * Common trait for stack-length -based sided / insets-like classes
  * @tparam Repr Type of concrete implementation of this trait
 * @author Mikko Hilpinen
  * @since 7.9.2025, v1.6
  */
trait StackInsetsLike[+Repr] extends ScalableSidesLike[StackLength, StackSize, Repr]
{
	// COMPUTED	---------------------------
	
	/**
	  * @return A copy of these insets where each side is marked with low priority
	  */
	def withLowPriority = map { _.lowPriority }
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
	
	/**
	  * @return A copy of these insets with no minimum value (all minimum values at 0)
	  */
	def noMin = map { _.noMin }
	/**
	  * @return A copy of these insets with no maximum value
	  */
	def noMax = map { _.noMax }
	/**
	  * @return A copy of these insets with no minimum or maximum value
	  */
	def noLimits = map { _.noLimits }
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def zeroLength = StackLength.fixedZero
	
	override def total: StackSize = StackSize(totalAlong(X), totalAlong(Y))
	
	override protected def join(a: StackLength, b: StackLength): StackLength = a + b
	override protected def subtract(from: StackLength, amount: StackLength): StackLength = from - amount
	override protected def multiply(a: StackLength, multiplier: Double)  = a * multiplier
	
	
	// OTHER	---------------------------
	
	/**
	  * @param amount Length increase affecting each side of these insets
	  * @return A copy of these insets with each side increased
	  */
	def +(amount: Double) = if (amount == 0) self else map { _ + amount }
	/**
	  * @param other A set of insets
	  * @return A copy of these insets which have been increased by the other set of insets
	  */
	def +(other: Insets) =
		withSides((sides.keySet ++ other.sides.keySet).view.map { dir => dir -> (apply(dir) + other(dir)) }.toMap)
	/**
	  * @param amount Length decrease affecting each side of these insets
	  * @return A copy of these insets with each side decreased
	  */
	def -(amount: Double) = if (amount == 0) self else map { _ - amount }
	/**
	  * @param other A set of insets
	  * @return A copy of these insets with the specified insets subtracted on all sides
	  */
	def -(other: Insets) = this + (-other)
	
	/**
	  * @param other Another set of insets
	  * @return Combination between these insets, which attempts to fulfill the conditions in both
	  */
	def &&(other: StackInsets) = mergeWith(other) { _ && _ }
	/**
	  * @param other Another set of insets
	  * @return A combination of these insets that selects the smaller applicable value
	  */
	def min(other: StackInsets) = mergeWith(other) { _ min _ }
	/**
	  * @param other Another set of insets
	  * @return A combination of these insets that selects the larger applicable value
	  */
	def max(other: StackInsets) = mergeWith(other) { _ max _ }
	
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
	def expandingAccordingTo(axis: Axis2D, alignment: Alignment) = alignment(axis).direction match {
		case preservedDirection: Sign => expandingTowards(axis.toDirection(preservedDirection.opposite))
		case Neutral => expandingAlong(axis)
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