package utopia.firmament.model.stack

import utopia.firmament.controller.Stacker
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.stack
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.sign.{Sign, SignOrZero}
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.{Alignment, Axis2D, LinearAlignment}
import utopia.paradigm.measurement.{Distance, Ppi}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape2d.insets.Insets

/**
  * These extensions allow easier creation of stack lengths & stack sizes
  * @author Mikko Hilpinen
  * @since 26.4.2019, Reflection v1+
  */
object LengthExtensions
{
	implicit class LengthNumber[A](val i: A) extends AnyVal
	{
		private def double(implicit n: Numeric[A]) = n.toDouble(i)
		
		/**
		 * @return A stacklength that has no maximum or minimum, preferring this length
		 */
		def any(implicit n: Numeric[A]) = StackLength.any(double)
		/**
		 * @return A stacklength fixed to this length
		 */
		def fixed(implicit n: Numeric[A]) = StackLength.fixed(double)
		
		/**
		 * @return A stacklength maximized on this length with no minimum
		 */
		def downscaling(implicit n: Numeric[A]) = StackLength.downscaling(double)
		/**
		 * @return A stacklength minimized on this length with no maximum
		 */
		def upscaling(implicit n: Numeric[A]) = StackLength.upscaling(double)
		
		/**
		 * @param max Maximum length
		 * @return A stack length between this and maximum, preferring this
		 */
		def upTo(max: Double)(implicit n: Numeric[A]) = StackLength(double, double, max)
		/**
		 * @param min Minimum length
		 * @return A stack length between minimum and this, preferring this
		 */
		def downTo(min: Double)(implicit n: Numeric[A]) = StackLength(min, double, double)
	}
	
	implicit class StackableDistance(val d: Distance) extends AnyVal
	{
		/**
		 * @return A stack length that has no maximum or minimum, preferring this length
		 */
		def any(implicit ppi: Ppi) = StackLength.any(d.toPixels)
		
		/**
		 * @return A stack length fixed to this length
		 */
		def fixed(implicit ppi: Ppi) = StackLength.fixed(d.toPixels)
		
		/**
		 * @return A stack length maximized on this length with no minimum
		 */
		def downscaling(implicit ppi: Ppi) = StackLength.downscaling(d.toPixels)
		
		/**
		 * @return A stack length minimized on this length with no maximum
		 */
		def upscaling(implicit ppi: Ppi) = StackLength.upscaling(d.toPixels)
		
		/**
		 * @param max Maximum length
		 * @return A stack length between this and maximum, preferring this
		 */
		def upTo(max: Distance)(implicit ppi: Ppi) =
		{
			val p = d.toPixels
			StackLength(p, p, max.toPixels)
		}
		
		/**
		 * @param min Minimum length
		 * @return A stack length between minimum and this, preferring this
		 */
		def downTo(min: Distance)(implicit ppi: Ppi) =
		{
			val p = d.toPixels
			StackLength(min.toPixels, p, p)
		}
	}
	
	implicit class StackConvertibleInsets(val i: Insets) extends AnyVal
	{
		/**
		 * @return A stack-compatible copy of these insets that supports any other value but prefers these
		 */
		def any = toStackInsets { _.any }
		
		/**
		 * @return A stack-compatible copy of these insets that only allows these exact insets
		 */
		def fixed = toStackInsets { _.fixed }
		
		/**
		 * @return A stack-compatible copy of these insets that allows these or smaller insets
		 */
		def downscaling = toStackInsets { _.downscaling }
		
		/**
		 * @return A stack-compatible copy of these insets that allows these or larger insets
		 */
		def upscaling = toStackInsets { _.upscaling }
		
		/**
		 * @param min Minimum set of insets
		 * @return A set of stack insets that allows values below these insets, down to specified insets
		 */
		def downTo(min: Insets) = toStackInsetsWith(min) { (opt, min) => opt.downTo(min) }
		
		/**
		 * @param max Maximum set of insets
		 * @return A set of stack insets that allows values above these insets, up to specified insets
		 */
		def upTo(max: Insets) = toStackInsetsWith(max) { (opt, max) => opt.upTo(max) }
		
		
		// OTHER    ---------------------
		
		/**
		 * Converts these insets to stack insets by using the specified function
		 * @param f A function for converting a static length to a stack length
		 * @return Stack insets based on these insets
		 */
		def toStackInsets(f: Double => StackLength) = StackInsets(i.sides.map { case (d, l) => d -> f(l) })
		
		/**
		 * Creates a set of stack insets by combining these insets with another set of insets and using the specified merge function
		 * @param other Another set of insets
		 * @param f Function for producing stack lengths
		 * @return A new set of stack insets
		 */
		def toStackInsetsWith(other: Insets)(f: (Double, Double) => StackLength) = stack.StackInsets(
			(i.sides.keySet ++ other.sides.keySet).map { d => d -> f(i(d), other(d)) }.toMap)
	}
	
	implicit class StackConvertibleSize(val s: Size) extends AnyVal
	{
		/**
		  * @return A stack size where this is the optimal size, but the actual size may vary freely
		  */
		def any = StackSize.any(s)
		/**
		  * @return A stack size that matches this size exactly
		  */
		def fixed = StackSize.fixed(s)
		
		/**
		  * @return A stack size where this is the optimal and maximum size
		  */
		def downscaling = StackSize.downscaling(s)
		/**
		  * @return A stack size where this is the optimal and minimum size
		  */
		def upscaling = StackSize.upscaling(s)
		
		/**
		  * @param max Maximum size
		  * @return A stack size where this is the optimal and minimum size, and where the specified maximum applies
		  */
		def upTo(max: Size) = StackSize(s, s, Some(max))
		/**
		  * @param min Minimum size
		  * @return A stack size where this is the optimal and maximum size, and where the specified minimum applies
		  */
		def downTo(min: Size) = StackSize(min, s, Some(s))
	}
	
	implicit class StackingLinearAlignment(val a: LinearAlignment) extends AnyVal
	{
		/**
		  * @return A stack layout based on this alignment
		  */
		def toStackLayout: StackLayout = StackLayout.aligning(a)
	}
	
	// NB: At the time of writing, when this extension was added (10.8.2022, at the introduction of the Paradigm module)
	// These functions were copied from the Reflection Alignment type pretty much as they were
	// May need refactoring
	implicit class StackingAlignment(val a: Alignment) extends AnyVal
	{
		/**
		  * Positions the specified area within a set of bounds so that it follows this alignment
		  * @param areaToPosition Size of the area/element to position (Eg. content size)
		  * @param within The bounds within which the area is positioned (Eg. component bounds)
		  * @param insets Insets used around the positioned content (default = 0 on each side).
		  *                If the content wouldn't fit into the target area with these margins, the margins are decreased.
		  *                The maximum amounts in the margins are ignored.
		  * @param fitWithinBounds Whether the resulting bounds should be fit within the 'within' bounds (default = true).
		  *                        Only used when the 'areaToPosition' doesn't naturally fit into the specified bounds.
		  * @param preserveShape Whether this algorithm should preserve the shape of the area when the size need to be
		  *                      shrunk in order to fit within the specified bounds (default = true). Only used when
		  *                      'fitWithinBounds' -parameter is set to true.
		  * @return A set of bounds for the area to position that follows this alignment
		  */
		def positionWithInsets(areaToPosition: Size, within: Bounds, insets: StackInsetsConvertible = StackInsets.any,
		             fitWithinBounds: Boolean = true, preserveShape: Boolean = true) =
		{
			// Calculates the final size of the positioned area
			val fittedArea = {
				if (fitWithinBounds) {
					if (preserveShape) {
						val scale = (within.size / areaToPosition).minDimension
						if (scale < 1)
							areaToPosition * scale
						else
							areaToPosition
					}
					else {
						val scale = (within.size / areaToPosition).map { _ min 1 }
						areaToPosition * scale
					}
				}
				else
					areaToPosition
			}
			
			// Calculates the new position for the area
			val topLeft = Point.fromFunction2D { axis =>
				val (startMargin, endMargin) = insets.toInsets.sidesAlong(axis).toTuple
				within.position(axis) + positionWithDirection(fittedArea(axis), within.size(axis),
					startMargin, endMargin, a(axis).direction, !fitWithinBounds)
			}
			
			Bounds(topLeft, fittedArea)
		}
		
		/**
		  * Positions an area and streches it along the align side. I.e. If called for left alignment, places the item
		  * at the left side of the 'within' parameter and streches it to cover the vertical area, if possible.
		  * Notice that the area won't be streched for center or corner alignments
		  * @param areaToPosition Size of the area to position (minimum and maximum sizes are respected when possible)
		  * @param within Bounds within which the area must be placed
		  * @param insets Insets to place around the area (default = any, preferring zero)
		  * @return Positioned and streched area
		  */
		def positionStretching(areaToPosition: StackSize, within: Bounds, insets: StackInsetsConvertible = StackInsets.any) =
		{
			val actualInsets = insets.toInsets
			
			// Some alignments can't stretch components properly
			if (a.directions.size != 1)
				positionWithInsets(areaToPosition.optimal, within, actualInsets, preserveShape = false)
			else {
				// Calculates the new size of the area
				val sideAxis = a.directions.head.axis
				val strechAxis = sideAxis.perpendicular
				
				val areaWithinDefaultInsets = within - actualInsets.optimal
				val areaWithinMinInsets = within - actualInsets.min
				
				// Calculates new breadth
				val breadth = areaToPosition(sideAxis)
				val maxBreadth = areaWithinMinInsets.size(sideAxis)
				val actualBreadth = {
					// Case: Margin has to be shrunk below minimum
					if (breadth.min > maxBreadth)
						breadth.min min within.size(sideAxis)
					// Case: breadth within limits
					else
						breadth.optimal min maxBreadth
				}
				
				val length = areaToPosition(strechAxis)
				val targetLength = areaWithinDefaultInsets.size(strechAxis)
				
				length.max.filter { _ < targetLength } match {
					// Case: The area can't be streched far enough => streches as far as possible, then aligns
					case Some(maxLength) =>
						positionWithInsets(Size(maxLength, actualBreadth, strechAxis), within, actualInsets,
							preserveShape = false)
					case None =>
						val maxLength = areaWithinMinInsets.size(strechAxis)
						val actualLength =
						{
							// Case: Margin has to be shrunk below minimum
							if (length.min > maxLength)
								length.min min within.size(strechAxis)
							// Case: Length is within limits
							else
								(length.optimal max targetLength) min maxLength
						}
						positionWithInsets(Size(actualLength, actualBreadth, strechAxis), within, actualInsets.noLimits,
							preserveShape = false)
				}
			}
		}
		
		/**
		  * Positions an area next to another area within a third area using this alignment
		  * @param areaToPosition Size of the area to position within the specified parameters.
		  *                       Maximum lengths are not used.
		  * @param referenceArea Area next to which this new area will be placed. Should at least overlap with the
		  *                      maximum bounds area (within)
		  * @param within The area within which the resulting bounds must fit
		  * @param optimalMargin Margin placed between the resulting bounds and the reference area
		  *                      when there is enough room (default = 0)
		  * @param primaryAxis The axis which is first considered when this alignment uses both axes. E.g. If X is used
		  *                    with the BottomRight alignment, the resulting area will be placed on the right first and
		  *                    then to the bottom (instead of being placed to the bottom first and then to the right).
		  *                    Default = Y = Vertical position will be considered first. This parameter is ignored when
		  *                    this alignment specifies 0 to 1 directions.
		  * @param avoidOverlap Whether overlap with the reference area should be avoided when possible. When true, the
		  *                     resulting bounds may be shrunk below optimal to avoid overlap with the reference area.
		  *                     However, overlap may still be used in order to ensure the minimum size of the area.
		  *                     Default = false.
		  * @param preserveShape Whether the shape of the positioned area should be preserved when resize is necessary.
		  *                      Default = false.
		  * @return Bounds that best match the specified requirements
		  */
		// TODO: Refactor utilizing the positiveRelativeToWithin that has already been written at Alignment
		def positionNextToWithin(areaToPosition: StackSize, referenceArea: Bounds, within: Bounds,
		                         optimalMargin: Double = 0.0, primaryAxis: Axis2D = Y, avoidOverlap: Boolean = false,
		                         preserveShape: Boolean = false) =
		{
			// May override the primary axis parameter
			val actualPrimaryAxis = if (a.affects(primaryAxis)) primaryAxis else primaryAxis.perpendicular
			a(actualPrimaryAxis).direction match {
				case primaryDirection: Sign =>
					val primaryLength = areaToPosition(actualPrimaryAxis)
					val optimalPrimaryLength = primaryLength.optimal
					
					// Positions along the primary axis first
					val (primaryCoordinate, assignedLength) = positionNextToPrimary(actualPrimaryAxis, primaryDirection,
						referenceArea, within, optimalPrimaryLength, primaryLength.min, optimalMargin, avoidOverlap)
					
					// Next positions along the secondary axis
					val secondaryAxis = actualPrimaryAxis.perpendicular
					val secondaryLength = areaToPosition(secondaryAxis)
					val optimalSecondaryLength =
					{
						val defaultOptimal = secondaryLength.optimal
						if (preserveShape)
							defaultOptimal * (assignedLength / optimalPrimaryLength)
						else
							defaultOptimal
					}
					val areaLength = within.size(secondaryAxis)
					
					// Case: Will fill the whole available area
					if (areaLength < optimalSecondaryLength)
					{
						val secondaryCoordinate = within.minAlong(secondaryAxis)
						if (preserveShape)
						{
							// The primary length is also affected when the secondary length has to be shrunk even more
							val additionalScaling = areaLength / optimalSecondaryLength
							val scaledPrimaryLength = assignedLength * additionalScaling
							val (primaryCoordinate, newPrimaryLength) = positionNextToPrimary(actualPrimaryAxis,
								primaryDirection, referenceArea, within, scaledPrimaryLength, scaledPrimaryLength,
								optimalMargin, avoidOverlap)
							Bounds(Point(primaryCoordinate, secondaryCoordinate, actualPrimaryAxis),
								Size(newPrimaryLength, areaLength, actualPrimaryAxis))
						}
						else
							Bounds(Point(primaryCoordinate, secondaryCoordinate, actualPrimaryAxis),
								Size(assignedLength, areaLength, actualPrimaryAxis))
					}
					// Case: Will be aligned along the secondary axis
					else
					{
						val referenceStart = referenceArea.minAlong(secondaryAxis)
						val referenceLength = referenceArea.size(secondaryAxis)
						val referenceEnd = referenceStart + referenceLength
						
						// Calculates the preferred aligned position
						val preferredSecondaryCoordinate = a(secondaryAxis).direction match {
							case Positive => referenceEnd - optimalSecondaryLength
							case Negative => referenceStart
							case Neutral => referenceStart + referenceLength / 2 - optimalSecondaryLength / 2
						}
						// Adjusts the position so that the component fits within the target area
						val minSecondary = within.minAlong(secondaryAxis)
						val maxSecondary = minSecondary + areaLength
						Bounds(Point(primaryCoordinate, (preferredSecondaryCoordinate max minSecondary) min maxSecondary,
							actualPrimaryAxis), Size(assignedLength, optimalSecondaryLength, actualPrimaryAxis))
					}
				
				// Case: Center alignment =>
				// Positions the area at the center of the reference area, using its optimal size (or lower)
				case Neutral => Bounds.centered(referenceArea.center, areaToPosition.optimal).fittedInto(within)
			}
		}
		
		/**
		  * Stretches and positions an area so that it is placed next to the specified target area and lies within
		  * the specified 'within' area, if possible.
		  *
		  * If the resulting area will share an edge with the target area
		  * (i.e. alignment Left, Right, Top or Bottom is used),
		  * attempts to match their lengths (respecting the input area's stack length limits).
		  *
		  * @param area The area to position, including maximum and minimum size of the resulting area
		  * @param to Area the 'area' will be placed next to
		  * @param within Area within which the resulting area should reside
		  * @param margin Margin placed between the resulting area and 'to', whenever possible
		  * @param swapToFit Whether this alignment may be switched to its opposite, in case that would help the
		  *                  resulting area to not overlap with 'to'
		  *
		  * @return An area that lies relative to the 'to' area according to this alignment (or its opposite) and,
		  *         if possible and applicable, shares an edge length with the 'to' area.
		  */
		def stretchNextToWithin(area: StackSize, to: Bounds, within: Bounds, margin: Double = 0.0,
		                        swapToFit: Boolean = false) =
		{
			// Matches the shared edge length as much as possible
			val defaultSize = a.directions.only match {
				// Case: One-directional alignment => Edge matching enabled
				case Some(direction) =>
					// Modifies the area to match the target area along one edge
					Size.fromFunction2D { axis =>
						// Case: Aligning axis (perpendicular to the edge) => No change
						if (direction.axis == axis)
							area.optimal(axis)
						// Case: Non-aligned axis (parallel to the edge) => Matches the edge (checks max size also)
						else {
							val target = to.size(axis)
							area(axis).max.filter { _ < target }.getOrElse(target)
						}
					}
				// Case: 0- or bi-directional alignment => Edge matching disabled
				case None => area.optimal
			}
			// Makes sure the area fits within the target area (if possible),
			// and that the minimum size of the area is respected
			val actualSize = defaultSize.fittingWithin(within.size).filling(area.min)
			a.positionRelativeToWithin(actualSize, to, within, margin, swapToFit)
		}
		
		private def positionWithDirection(length: Double, withinLength: Double, targetStartMargin: StackLength,
		                                  targetEndMargin: StackLength, direction: SignOrZero,
		                                  allowStartBelowZero: Boolean) =
		{
			val emptyLength = withinLength - length
			if (emptyLength < 0 && !allowStartBelowZero)
				0.0
			else {
				direction match {
					case direction: Sign =>
						// Checks how much margin can be used
						val totalTargetMargin = targetStartMargin.optimal + targetEndMargin.optimal
						if (direction.isPositive) {
							// Case: Enough space available
							if (totalTargetMargin <= emptyLength)
								emptyLength - targetEndMargin.optimal
							// Case: Going out of bounds
							else if (emptyLength <= 0) {
								if (allowStartBelowZero)
									emptyLength
								else
									0.0
							}
							// Case: Decreased margins
							else {
								val usedEndMargin = Stacker.adjustLengths(
									Pair(targetEndMargin, targetStartMargin), emptyLength).head
								emptyLength - usedEndMargin
							}
						}
						else {
							// Case: Enough space available
							if (totalTargetMargin <= emptyLength)
								targetStartMargin.optimal
							// Case: Going out of bounds
							else if (emptyLength <= 0)
								0.0
							// Case: Decreased margins
							else {
								val usedStartMargin = Stacker.adjustLengths(
									Pair(targetStartMargin, targetEndMargin), emptyLength).head
								usedStartMargin
							}
						}
					case Neutral =>
						val baseResult = (withinLength - length) / 2.0
						if (baseResult >= 0 || allowStartBelowZero)
							baseResult
						else
							0.0
				}
			}
		}
		
		// Calculates position and length along the primary axis
		// Returns position -> length
		private def positionNextToPrimary(axis: Axis2D, preferredDirection: Sign, referenceArea: Bounds,
		                                  within: Bounds, optimalLength: Double, minLength: Double, optimalMargin: Double,
		                                  avoidOverlap: Boolean): (Double, Double) =
		{
			// Checks which side fits the component better
			val referenceAreaStart = referenceArea.minAlong(axis)
			val referenceAreaEnd = referenceArea.maxAlong(axis)
			val totalAreaLength = within.size(axis)
			val withinStart = within.minAlong(axis)
			val withinEnd = withinStart + totalAreaLength
			
			val areaBefore = (referenceAreaStart - withinStart) max 0
			val areaAfter = (withinEnd - referenceAreaEnd) max 0
			// Primary direction is listed first so that it is preferred
			val areas = Pair(Positive -> areaAfter, Negative -> areaBefore)
				.sortBy { _._1 != preferredDirection }
			
			val preferredLength = optimalLength + optimalMargin
			
			val (actualDirection, area) = areas.find { _._2 >= preferredLength }
				.orElse { areas.find { _._2 >= optimalLength } }
				.getOrElse { areas.maxBy { _._2 } }
			
			// Calculates the position and the size along the primary axis
			// Case: There is enough room for the component and the margin without overlap
			if (area >= preferredLength)
				(actualDirection match
				{
					case Positive => referenceAreaEnd + optimalMargin
					case Negative => referenceAreaStart - preferredLength
				}) -> optimalLength
			// Case: There is enough room for the component but not for the proper margin
			else if (area >= optimalLength)
				(actualDirection match
				{
					case Positive => withinEnd - optimalLength
					case Negative => withinStart
				}) -> optimalLength
			if (avoidOverlap)
			{
				// Case: Can avoid overlap by shrinking the component
				if (area >= minLength)
					(actualDirection match
					{
						case Positive => referenceAreaEnd
						case Negative => withinStart
					}) -> area
				// Case: Whole area still has to be used
				else if (totalAreaLength <= minLength)
					withinStart -> totalAreaLength
				// Case: Overlap can't be completely avoided
				else
					(actualDirection match
					{
						case Positive => withinEnd - minLength
						case Negative => withinStart
					}) -> minLength
			}
			else
			{
				// Case: Overlap required, but component size can be preserved
				if (totalAreaLength >= optimalLength)
					(actualDirection match
					{
						case Positive => withinEnd - optimalLength
						case Negative => withinStart
					}) -> optimalLength
				// Case: Both overlap and component resize required
				else
					withinStart -> totalAreaLength
			}
		}
	}
}
