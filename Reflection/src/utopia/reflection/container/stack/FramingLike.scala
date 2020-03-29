package utopia.reflection.container.stack

import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.stack.{CachingStackable, Stackable}
import utopia.reflection.component.ComponentWrapper
import utopia.reflection.container.Container
import utopia.reflection.shape.{StackInsets, StackLength, StackSize}
import utopia.genesis.shape.Axis._

/**
  * Framings are containers that present a component with scaling 'frames', like a painting
  * @author Mikko Hilpinen
  * @since 26.4.2019, v1+
  */
trait FramingLike[C <: Stackable] extends SingleStackContainer[C] with ComponentWrapper with CachingStackable
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Insets placed around the component in this container
	  */
	def insets: StackInsets
	
	/**
	  * @return The underlying container of this framing
	  */
	protected def container: Container[C]
	
	
	// IMPLEMENTED	--------------------
	
	override def children = super[SingleStackContainer].children
	
	override protected def wrapped = container
	
	override def isVisible_=(isVisible: Boolean) = super[CachingStackable].isVisible_=(isVisible)
	
	override protected def updateVisibility(visible: Boolean) = super[ComponentWrapper].isVisible_=(visible)
	
	override def components = container.components
	
	// TODO: This doesn't get called correctly (maybe a problem with the stack hierarchy)?
	override def updateLayout() =
	{
		val c = content
		if (c.isDefined)
		{
			// Repositions and resizes content
			val layout = Axis2D.values.map
			{
				axis =>
					// Calculates lengths
					val (contentLength, topLeftMarginLength) = lengthsFor(c.get, axis)
					// Margin cannot go below 0
					if (topLeftMarginLength < 0)
						axis -> (0.0, lengthAlong(axis))
					else
						axis -> (topLeftMarginLength, contentLength)
			}.toMap
			
			val position = Point(layout(X)._1, layout(Y)._1)
			val size = Size(layout(X)._2, layout(Y)._2)
			
			c.get.bounds = Bounds(position, size)
		}
	}
	
	override def calculatedStackSize = content.map { _.stackSize + insets.total } getOrElse StackSize.any
	
	
	// OTHER	-----------------------
	
	// Returns content final length -> top/left margin final length
	private def lengthsFor(content: C, axis: Axis2D) =
	{
		val myLength = lengthAlong(axis)
		val contentLength = content.stackSize.along(axis)
		val (firstInset, secondInset) = insets.sidesAlong(axis)
		
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
