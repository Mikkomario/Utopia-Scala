package utopia.firmament.component.container.single

import utopia.firmament.component.stack.{CachingStackable, Stackable}
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.Bounds

import scala.annotation.tailrec

/**
  * Framings are wrappers that present a component with scaling 'frames', like a painting
  * @author Mikko Hilpinen
  * @since 26.4.2019, Reflection v1
  */
trait FramingLike[+C <: Stackable] extends SingleContainer[C] with CachingStackable
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Insets placed around the component in this container
	  */
	def insets: StackInsets
	
	
	// IMPLEMENTED	--------------------
	
	override def updateLayout() = {
		// Repositions and resizes content
		// Makes sure that the content stays within the bounds of this framing (disabled for now, uncomment to enable)
		// val area = Bounds(Point.origin, size)
		content.bounds = Bounds.fromFunction2D(lengthsFor)// .fittedInto(area)
	}
	
	override def calculatedStackSize = content.stackSize + insets.total
	
	
	// OTHER	-----------------------
	
	// Returns the content position and length as a span
	private def lengthsFor(axis: Axis2D) = {
		val myLength = lengthAlong(axis)
		val contentLength = content.stackSize(axis)
		val axisInsets = insets(axis)
		
		val totalAdjustment = myLength - (contentLength.optimal + axisInsets.map { _.optimal }.sum)
		// Optimal position & optimal size
		val optimal = Pair(axisInsets.first.optimal, contentLength.optimal)
		
		// Actual position & actual size
		val applied = {
			// Case: No adjustment is necessary => Uses default lengths
			if (totalAdjustment == 0)
				optimal
			// Case: Adjustment is needed => Distributes it between content and the insets
			else {
				val getMax = {
					// Case: Enlargement required
					if (totalAdjustment > 0) { l: StackLength => l.max.map { _ - l.optimal } }
					// Case: Shrinking required
					else { l: StackLength => Some(l.optimal - l.min) }
				}
				// Calculates the applied adjustments (content + first inset)
				val adjustments = adjustmentsFor(contentLength, axisInsets, totalAdjustment)(getMax)
				// Returns adjusted values
				optimal.mergeWith(adjustments) { _ + _ }
			}
		}
		// Returns as a span
		NumericSpan(applied.first, applied.sum)
	}
	
	// Returns adjustment to position (based on the first inset) and adjustment to content length
	private def adjustmentsFor(contentLength: StackLength, insets: Pair[StackLength], totalAdjustment: Double)
	                          (getMaxAdjust: StackLength => Option[Double]): Pair[Double] =
	{
		// First adjusts between content and total margins
		val (contentAdjust, totalInsetsAdjust) = distributeAdjustmentBetween(
			Pair(contentLength, insets.merge { _ + _ }), totalAdjustment)(getMaxAdjust).toTuple
		// Then adjusts between individual inset sides
		val firstInsetAdjust = {
			if (totalInsetsAdjust == 0)
				0.0
			else
				distributeAdjustmentBetween(insets, totalInsetsAdjust)(getMaxAdjust).first
		}
		Pair(firstInsetAdjust, contentAdjust)
	}
	
	// Returns adjustments with the correct sign
	private def distributeAdjustmentBetween(lengths: Pair[StackLength], adjustment: Double)
										   (getMaxAdjust: StackLength => Option[Double]): Pair[Double] =
	{
		// Determines the default split based on length priorities
		val priorities = lengths.map { _.priority.isFirstAdjustedBy(adjustment) }
		val maxAdjustments = lengths.map(getMaxAdjust)
		val target = adjustment.abs
		distributeAdjustmentBetween(priorities, Pair.twice(0.0), maxAdjustments, target, target)
			.map { _ * adjustment.sign }
	}
	
	// Expects and returns adjustments as absolute values
	// In priorities, true means low priority (expand first) and false means high priority (expand last)
	@tailrec
	private def distributeAdjustmentBetween(priorities: Pair[Boolean], placedAdjustments: Pair[Double],
	                                        maxAdjustments: Pair[Option[Double]], targetAdjustment: Double,
	                                        remainingAdjustment: Double): Pair[Double] =
	{
		// Splits the remaining adjustment without regard to maximums first
		val defaultAdjustments = {
			// Case: Equal priority => Splits the adjustment
			if (priorities.isSymmetric)
				placedAdjustments.map { _ + remainingAdjustment / 2 }
			// Case: Unequal priority => Adjusts one of the components first
			else
				priorities.mergeWith(placedAdjustments) { (isLow, placed) =>
					if (isLow) placed + remainingAdjustment else placed
				}
		}
		// If content (first) maximum is reached, puts the remaining adjustment to margin (second)
		// This, except when both maximums are reached
		val maxStatus = defaultAdjustments.mergeWith(maxAdjustments) { (default, max) => max.exists { _ < default } }
		
		// Case: No maximizing or both have been maximized => Implements the default distribution
		if (maxStatus.forall { !_ } || maxStatus.isSymmetric)
			defaultAdjustments
		// Case: One is maximized => Applies the maximum default(s) at least
		else {
			val maxAdjusted = defaultAdjustments.mergeWith(maxAdjustments) { (target, max) =>
				max match {
					case Some(max) => target min max
					case None => target
				}
			}
			val stillRemaining = targetAdjustment - maxAdjusted.sum
			// Case: Some adjustment is still required => Uses recursion
			if (stillRemaining > 0)
				distributeAdjustmentBetween(priorities.mergeWith(maxStatus) { _ && !_ }, maxAdjusted, maxAdjustments,
					targetAdjustment, stillRemaining)
			// Case: No more adjustment required => Completes
			else
				maxAdjusted
		}
	}
}
