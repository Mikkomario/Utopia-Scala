package utopia.firmament.component.container.single

import utopia.firmament.component.stack.{CachingStackable, Stackable}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.firmament.model.stack.{StackInsets, StackLength}

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
	
	override def updateLayout() =
	{
		// Repositions and resizes content
		val layout = Axis2D.values.map { axis =>
			// Calculates lengths
			val (contentLength, topLeftMarginLength) = lengthsFor(axis)
			// Margin cannot go below 0
			if (topLeftMarginLength < 0)
				axis -> (0.0, lengthAlong(axis))
			else
				axis -> (topLeftMarginLength, contentLength)
		}.toMap
		
		val position = Point(layout(X)._1, layout(Y)._1)
		val size = Size(layout(X)._2, layout(Y)._2)
		
		content.bounds = Bounds(position, size)
	}
	
	override def calculatedStackSize = content.stackSize + insets.total
	
	
	// OTHER	-----------------------
	
	// Returns content final length -> top/left margin final length
	private def lengthsFor(axis: Axis2D) =
	{
		val myLength = lengthAlong(axis)
		val contentLength = content.stackSize.along(axis)
		val (firstInset, secondInset) = insets.sidesAlong(axis).toTuple
		
		val totalAdjustment = myLength - (contentLength.optimal + firstInset.optimal + secondInset.optimal)
		
		// Sometimes adjustment isn't necessary
		if (totalAdjustment == 0)
			contentLength.optimal -> firstInset.optimal
		else
		{
			// Either enlarges or shrinks the components
			if (totalAdjustment > 0)
			{
				val (contentAdjust, firstInsetAdjust) = adjustmentsFor(contentLength, firstInset, secondInset,
					totalAdjustment) { l => l.max.map { _ - l.optimal } }
				
				(contentLength.optimal + contentAdjust) -> (firstInset.optimal + firstInsetAdjust)
			}
			else
			{
				val (contentAdjust, firstInsetAdjust) = adjustmentsFor(contentLength, firstInset, secondInset,
					totalAdjustment) { l => Some(l.optimal - l.min) }
				
				(contentLength.optimal - contentAdjust) -> (firstInset.optimal - firstInsetAdjust)
			}
		}
	}
	
	// Returns content adjustment -> first inset adjustment (with correct multiplier)
	private def adjustmentsFor(contentLength: StackLength, firstInset: StackLength, secondInset: StackLength,
							   totalAdjustment: Double)(getMaxAdjust: StackLength => Option[Double]): (Double, Double) =
	{
		// First adjusts between content and total margins
		val (contentAdjust, totalInsetsAdjust) = distributeAdjustmentBetween(contentLength, firstInset + secondInset,
			totalAdjustment, treatAsEqual = false)(getMaxAdjust)
		// Then adjusts between individual inset sides
		val (firstInsetAdjust, _) =
		{
			if (totalInsetsAdjust == 0)
				0.0 -> 0.0
			else
				distributeAdjustmentBetween(firstInset, secondInset, totalInsetsAdjust, treatAsEqual = true)(getMaxAdjust)
		}
		
		// Returns content adjust + first inset adjust
		contentAdjust -> firstInsetAdjust
	}
	
	private def distributeAdjustmentBetween(firstLength: StackLength, secondLength: StackLength, adjustment: Double,
											treatAsEqual: Boolean)
										   (getMaxAdjust: StackLength => Option[Double]): (Double, Double) =
	{
		// Determines the default split based on length priorities
		val firstPriority = firstLength.priority.isFirstAdjustedBy(adjustment)
		val secondPriority = secondLength.priority.isFirstAdjustedBy(adjustment)
		val (defaultFirstAdjust, defaultSecondAdjust) =
		{
			if (firstPriority == secondPriority)
				(adjustment.abs / 2) -> (adjustment.abs / 2)
			else if (firstPriority)
				adjustment.abs -> 0.0
			else
				0.0 -> adjustment
		}
		
		val firstMaxAdjust = getMaxAdjust(firstLength)
		
		// If content maximum is reached, puts the remaining adjustment to margin
		if (firstMaxAdjust.exists { defaultFirstAdjust >= _ })
		{
			val remainsAfterMaxed = defaultFirstAdjust - firstMaxAdjust.get
			firstMaxAdjust.get -> (defaultSecondAdjust + remainsAfterMaxed)
		}
		else
		{
			val secondMaxAdjust = getMaxAdjust(secondLength)
			
			// If margin maximum is reached, puts the remaining adjustment to component
			// (until maxed, after which puts to margin anyway (or to both if treated equally))
			if (secondMaxAdjust.exists { defaultSecondAdjust >= _ })
			{
				val remainsAfterSecondMaxed = defaultSecondAdjust - secondMaxAdjust.get
				val proposedFirstAdjust = defaultFirstAdjust + remainsAfterSecondMaxed
				
				if (firstMaxAdjust.exists { proposedFirstAdjust > _ })
				{
					val remainsAfterFirstMaxed = proposedFirstAdjust - firstMaxAdjust.get
					if (treatAsEqual)
						(firstMaxAdjust.get + remainsAfterFirstMaxed / 2) -> (secondMaxAdjust.get + remainsAfterFirstMaxed / 2)
					else
						firstMaxAdjust.get -> (secondMaxAdjust.get + remainsAfterFirstMaxed)
				}
				else
				{
					proposedFirstAdjust -> secondMaxAdjust.get
				}
			}
			// If neither is reached, adjusts both equally
			else
				defaultFirstAdjust -> defaultSecondAdjust
		}
	}
}
