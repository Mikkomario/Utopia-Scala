package utopia.reflection.shape

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import javax.swing.SwingConstants
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Direction2D, HorizontalDirection, Point, Size, VerticalDirection}
import utopia.reflection.container.stack.Stacker
import utopia.reflection.shape.stack.{StackInsets, StackInsetsConvertible, StackLength, StackSize}

import scala.collection.immutable.HashMap

object Alignment
{
	/**
	  * Common trait for alignments that only represent a single direction
	  */
	sealed trait SingleDirectionAlignment[+Direction <: Direction2D] extends Alignment
	{
		// ABSTRACT	---------------------
		
		/**
		  * @return The direction matching this alignment
		  */
		def direction: Direction
		
		
		// COMPUTED	---------------------
		
		/**
		  * @return Whether this alignment moves items to the positive (+) direction on the specified axis
		  */
		@deprecated("Please use sign instead", "v1.2.1")
		def isPositiveDirection = sign.isPositive
		
		/**
		  * @return Axis used by this alignment
		  */
		def axis = direction.axis
		
		/**
		  * @return The directional sign of this alignment
		  */
		def sign = direction.sign
		
		
		// IMPLEMENTED	-----------------
		
		override def supportedAxes = Set(axis)
		
		override def directions = Vector(direction)
	}
	
	/**
	  * Common trait for alignments that only represent a horizontal direction
	  */
	sealed trait HorizontalAlignment extends SingleDirectionAlignment[HorizontalDirection]
	{
		// IMPLEMENTED	-------------------
		
		override def vertical = Center
		
		override def horizontalDirection = Some(direction)
		
		override def verticalDirection = None
	}
	
	/**
	  * Common trait for alignments that only represent a vertical direction
	  */
	sealed trait VerticalAlignment extends SingleDirectionAlignment[VerticalDirection]
	{
		// IMPLEMENTED	-------------------
		
		override def horizontal = Center
		
		override def horizontalDirection = None
		
		override def verticalDirection = Some(direction)
	}
	
	/**
	  * In left alignment, content is leading
	  */
	case object Left extends HorizontalAlignment
	{
		override def direction = Direction2D.Left
		
		override def opposite = Right
		
		override def swingComponents = HashMap(X -> SwingConstants.LEADING)
		
		override def horizontal = this
	}
	
	/**
	  * In right alignment, content is trailing
	  */
	case object Right extends HorizontalAlignment
	{
		override def direction = Direction2D.Right
		
		override def opposite = Left
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING)
		
		override def horizontal = this
	}
	
	/**
	  * In top alignment, content is positioned at the the top of a component
	  */
	case object Top extends VerticalAlignment
	{
		override def direction = Direction2D.Up
		
		override def opposite = Bottom
		
		override def swingComponents = HashMap(Y -> SwingConstants.TOP)
		
		override def vertical = this
	}
	
	/**
	  * In bottom alignment, content is positioned at the bottom of a component
	  */
	case object Bottom extends VerticalAlignment
	{
		override def direction = Direction2D.Down
		
		override def opposite = Top
		
		override def swingComponents = HashMap(Y -> SwingConstants.BOTTOM)
		
		override def vertical = this
	}
	
	/**
	  * In center alignment, content is positioned at the center of a component
	  */
	case object Center extends Alignment
	{
		override val supportedAxes = Set(X, Y)
		
		override def opposite = this
		
		override def swingComponents = HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.CENTER)
		
		override def horizontal = this
		
		override def vertical = this
		
		override def directions = Vector[Direction2D]()
		
		override def horizontalDirection = None
		
		override def verticalDirection = None
	}
	
	case object TopLeft extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = BottomRight
		
		override def swingComponents = HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.TOP)
		
		override def horizontal = Left
		
		override def vertical = Top
		
		override def directions = Vector(Direction2D.Up, Direction2D.Left)
		
		override def horizontalDirection = Some(Direction2D.Left)
		
		override def verticalDirection = Some(Direction2D.Up)
	}
	
	case object TopRight extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = BottomLeft
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.TOP)
		
		override def horizontal = Right
		
		override def vertical = Top
		
		override def directions = Vector(Direction2D.Up, Direction2D.Right)
		
		override def horizontalDirection = Some(Direction2D.Right)
		
		override def verticalDirection = Some(Direction2D.Up)
	}
	
	case object BottomLeft extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = TopRight
		
		override def swingComponents = HashMap(X -> SwingConstants.LEADING, Y -> SwingConstants.BOTTOM)
		
		override def horizontal = Left
		
		override def vertical = Bottom
		
		override def directions = Vector(Direction2D.Down, Direction2D.Left)
		
		override def horizontalDirection = Some(Direction2D.Left)
		
		override def verticalDirection = Some(Direction2D.Down)
	}
	
	case object BottomRight extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = TopLeft
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.BOTTOM)
		
		override def horizontal = Right
		
		override def vertical = Bottom
		
		override def directions = Vector(Direction2D.Down, Direction2D.Right)
		
		override def horizontalDirection = Some(Direction2D.Right)
		
		override def verticalDirection = Some(Direction2D.Down)
	}
	
	
	// ATTRIBUTES	--------------------
	
	/**
	  * All possible values for Alignment
	  */
	val values = Vector(Left, Right, Top, Bottom, Center, TopLeft, TopRight, BottomLeft, BottomRight)
	
	/**
	  * All horizontally supported values
	  */
	val horizontal = Vector(Left, Center, Right)
	
	/**
	  * All vertically supported values
	  */
	val vertical = Vector(Top, Center, Bottom)
	
	
	// OTHER	-----------------------
	
	/**
	  * @param direction Target direction
	  * @return An alignment that places items to that direction
	  */
	def forDirection(direction: Direction2D) = direction match
	{
		case Direction2D.Up => Top
		case Direction2D.Down => Bottom
		case Direction2D.Right => Right
		case Direction2D.Left => Left
	}
	
	/**
	  * @param alignment Searched swing alignment
	  * @return A horizontal alignment matching the specified swing alignment. None if no alignment matched.
	  */
	def forHorizontalSwingAlignment(alignment: Int): Option[Alignment] = horizontal.find { _.swingComponents.get(X).contains(alignment) }
	
	/**
	  * @param alignment Searched swing alignment
	  * @return A vertical alignment matching the specified swing alignment. None if no alignment matched.
	  */
	def forVerticalSwingAlignment(alignment: Int): Option[Alignment] = vertical.find { _.swingComponents.get(Y).contains(alignment) }
	
	/**
	  * @param alignment Swing constant for alignment
	  * @return An alignment matchin the swing constant. None if no alignment matches the constant.
	  */
	def forSwingAlignment(alignment: Int): Option[Alignment] = forHorizontalSwingAlignment(alignment) orElse
		forVerticalSwingAlignment(alignment)
	
	/**
	  * Finds an alignment that matches the specified swing alignment combo
	  * @param horizontal Horizontal swing alignment component
	  * @param vertical Vertical swing alignment component
	  * @return A matching alignment
	  */
	def forSwingAlignments(horizontal: Int, vertical: Int): Alignment =
	{
		val hMatch = forHorizontalSwingAlignment(horizontal).getOrElse(Center)
		val vMatch = forVerticalSwingAlignment(vertical).getOrElse(Center)
		
		vMatch match
		{
			case Top =>
				hMatch match
				{
					case Left => TopLeft
					case Right => TopRight
					case _ => Top
				}
			case Bottom =>
				hMatch match
				{
					case Left => BottomLeft
					case Right => BottomRight
					case _ => Bottom
				}
			case _ => hMatch
		}
	}
}

/**
  * Alignments are used for specifying content position when there's additional room
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
sealed trait Alignment
{
	import Alignment._
	
	// ABSTRACT	----------------
	
	/**
	  * @return The axes supported for this alignment
	  */
	def supportedAxes: Set[Axis2D]
	
	/**
	  * @return The opposite for this alignment, if there is one
	  */
	def opposite: Alignment
	
	/**
	  * @return Swing representation(s) of this alignment, may be horizontal and/or vertical
	  */
	def swingComponents: Map[Axis2D, Int]
	
	/**
	  * @return A version of this alignment that only affects the horizontal axis
	  */
	def horizontal: Alignment
	/**
	  * @return A version of this alignment that only affects the vertical axis
	  */
	def vertical: Alignment
	
	/**
	  * @return Directions this alignment will try to move contents
	  */
	def directions: Vector[Direction2D]
	
	/**
	  * @return The direction this alignment will move the items horizontally. None if this alignment doesn't
	  *         specify a direction (centered)
	  */
	def horizontalDirection: Option[HorizontalDirection]
	/**
	  * @return The direction this alignment will move the items vertically. None if this alignment doesn't
	  *         specify a direction (centered)
	  */
	def verticalDirection: Option[VerticalDirection]
	
	
	// COMPUTED	----------------
	
	/**
	  * @return Whether this alignment can be used for horizontal axis (X)
	  */
	def isHorizontal = affects(X)
	/**
	  * @return Whether this alignment can be used for vertical axis (Y)
	  */
	def isVertical = affects(Y)
	
	/**
	  * @return Either positive, negative or None, based on whether this alignment will move items right, left
	  *         or to neither direction
	  */
	def horizontalDirectionSign = horizontalDirection.map { _.sign }
	/**
	  * @return Either positive, negative or None, based on whether this alignment will move items down, up
	  *         or to neither direction
	  */
	def verticalDirectionSign = verticalDirection.map { _.sign }
	
	
	// OTHER	----------------
	
	/**
	  * @param axis An axis
	  * @return Whether this alignment specifies a direction along that axis
	  */
	def affects(axis: Axis2D) = supportedAxes.contains(axis)
	
	/**
	  * @param axis Target axis
	  * @return A version of this alignment that can be used for the specified axis
	  */
	def along(axis: Axis2D) = if (affects(axis)) this else Center
	/**
	  * @param axis Target axis
	  * @return Direction determined by this alignment on specified axis. None if this alignment doesn't specify a
	  *         direction along specified axis (centered)
	  */
	def directionAlong(axis: Axis2D) = directions.find { _.axis == axis }.map { _.sign }
	
	/**
	  * Calculates the desired coordinate for an element along the specified axis
	  * @param axis Targeted axis
	  * @param elementLength The length of the targeted element along that axis
	  * @param areaLength The total length of the available area along that axis
	  * @return The proposed (top or left) coordinate for the element along that axis
	  */
	def positionAlong(axis: Axis2D, elementLength: Double, areaLength: Double) = directionAlong(axis) match
	{
		// Case: Specifies the preferred edge
		case Some(direction) =>
			direction match
			{
				// Case: Move as far as possible
				case Positive => areaLength - elementLength
				// Case: Move as little as possible
				case Negative => 0.0
			}
		// Case: Doesn't specify an edge => places the element at the center
		case None => (areaLength - elementLength) / 2.0
	}
	
	/**
	  * Calculates the desired x-coordinate for an element
	  * @param elementWidth The width of the targeted element
	  * @param areaWidth The total width of the available area
	  * @return The proposed (left) x-coordinate for the element
	  */
	def x(elementWidth: Double, areaWidth: Double) = positionAlong(X, elementWidth, areaWidth)
	/**
	  * Calculates the desired y-coordinate for an element
	  * @param elementHeight The height of the targeted element
	  * @param areaHeight The total height of the available area
	  * @return The proposed (top) y-coordinate for the element
	  */
	def y(elementHeight: Double, areaHeight: Double) = positionAlong(Y, elementHeight, areaHeight)
	
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
	def position(areaToPosition: Size, within: Bounds, insets: StackInsetsConvertible = StackInsets.any,
				 fitWithinBounds: Boolean = true, preserveShape: Boolean = true) =
	{
		// Calculates the final size of the positioned area
		val fittedArea =
		{
			if (fitWithinBounds)
			{
				if (preserveShape)
				{
					val scale = (within.size / areaToPosition).minDimension
					if (scale < 1)
						areaToPosition * scale
					else
						areaToPosition
				}
				else
				{
					val scale = (within.size / areaToPosition).map { _ min 1 }
					areaToPosition * scale
				}
			}
			else
				areaToPosition
		}
		
		// Calculates the new position for the area
		val topLeft = Point.calculateWith { axis =>
			val (startMargin, endMargin) = insets.toInsets.sidesAlong(axis)
			within.position.along(axis) + positionWithDirection(fittedArea.along(axis), within.size.along(axis),
				startMargin, endMargin, directionAlong(axis), !fitWithinBounds)
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
		if (directions.size != 1)
			position(areaToPosition.optimal, within, actualInsets, preserveShape = false)
		else
		{
			// Calculates the new size of the area
			val sideAxis = directions.head.axis
			val strechAxis = sideAxis.perpendicular
			
			val areaWithinDefaultInsets = within - actualInsets.optimal
			val areaWithinMinInsets = within - actualInsets.min
			
			// Calculates new breadth
			val breadth = areaToPosition.along(sideAxis)
			val maxBreadth = areaWithinMinInsets.size.along(sideAxis)
			val actualBreadth =
			{
				// Case: Margin has to be shrunk below minimum
				if (breadth.min > maxBreadth)
					breadth.min min within.size.along(sideAxis)
				// Case: breadth within limits
				else
					breadth.optimal min maxBreadth
			}
			
			val length = areaToPosition.along(strechAxis)
			val targetLength = areaWithinDefaultInsets.size.along(strechAxis)
			
			length.max.filter { _ < targetLength } match
			{
				// Case: The area can't be streched far enough => streches as far as possible, then aligns
				case Some(maxLength) =>
					position(Size(maxLength, actualBreadth, strechAxis), within, actualInsets, preserveShape = false)
				case None =>
					val maxLength = areaWithinMinInsets.size.along(strechAxis)
					val actualLength =
					{
						// Case: Margin has to be shrunk below minimum
						if (length.min > maxLength)
							length.min min within.size.along(strechAxis)
						// Case: Length is within limits
						else
							(length.optimal max targetLength) min maxLength
					}
					position(Size(actualLength, actualBreadth, strechAxis), within, actualInsets.noLimits,
						preserveShape = false)
			}
		}
	}
	
	/**
	  * Positions an area next to another area within a third area using this alignment
	  * @param areaToPosition Size of the area to position within the specified parameters.
	  *                       Maximum lengths are not used.
	  * @param referenceArea Area next to which this the new area will be placed. Should at least overlap with the
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
	def positionNextToWithin(areaToPosition: StackSize, referenceArea: Bounds, within: Bounds,
	                         optimalMargin: Double = 0.0, primaryAxis: Axis2D = Y, avoidOverlap: Boolean = false,
	                         preserveShape: Boolean = false) =
	{
		// May override the primary axis parameter
		val actualPrimaryAxis = if (affects(primaryAxis)) primaryAxis else primaryAxis.perpendicular
		directionAlong(actualPrimaryAxis) match
		{
			case Some(primaryDirection) =>
				val primaryLength = areaToPosition.along(actualPrimaryAxis)
				val optimalPrimaryLength = primaryLength.optimal
				
				// Positions along the primary axis first
				val (primaryCoordinate, assignedLength) = positionNextToPrimary(actualPrimaryAxis, primaryDirection,
					referenceArea, within, optimalPrimaryLength, primaryLength.min, optimalMargin, avoidOverlap)
				
				// Next positions along the secondary axis
				val secondaryAxis = actualPrimaryAxis.perpendicular
				val secondaryLength = areaToPosition.along(secondaryAxis)
				val optimalSecondaryLength =
				{
					val defaultOptimal = secondaryLength.optimal
					if (preserveShape)
						defaultOptimal * (assignedLength / optimalPrimaryLength)
					else
						defaultOptimal
				}
				val areaLength = within.size.along(secondaryAxis)
				
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
					val referenceLength = referenceArea.size.along(secondaryAxis)
					val referenceEnd = referenceStart + referenceLength
					
					// Calculates the preferred aligned position
					val preferredSecondaryCoordinate = directionAlong(secondaryAxis) match
					{
						case Some(direction) =>
							direction match
							{
								case Positive => referenceEnd - optimalSecondaryLength
								case Negative => referenceStart
							}
						case None => referenceStart + referenceLength / 2 - optimalSecondaryLength / 2
					}
					// Adjusts the position so that the component fits within the target area
					val minSecondary = within.minAlong(secondaryAxis)
					val maxSecondary = minSecondary + areaLength
					Bounds(Point(primaryCoordinate, (preferredSecondaryCoordinate max minSecondary) min maxSecondary,
						actualPrimaryAxis), Size(assignedLength, optimalSecondaryLength, actualPrimaryAxis))
				}
				
			// Case: Center alignment
			case None => Bounds.centered(referenceArea.center, areaToPosition.optimal).fittedInto(within)
		}
	}
	
	private def positionWithDirection(length: Double, withinLength: Double, targetStartMargin: StackLength,
									  targetEndMargin: StackLength, direction: Option[Sign],
									  allowStartBelowZero: Boolean) =
	{
		val emptyLength = withinLength - length
		if (emptyLength < 0 && !allowStartBelowZero)
			0.0
		else
		{
			direction match
			{
				case Some(definedDirection) =>
					// Checks how much margin can be used
					val totalTargetMargin = targetStartMargin.optimal + targetEndMargin.optimal
					if (definedDirection.isPositive)
					{
						// Case: Enough space available
						if (totalTargetMargin <= emptyLength)
							emptyLength - targetEndMargin.optimal
						// Case: Going out of bounds
						else if (emptyLength <= 0)
						{
							if (allowStartBelowZero)
								emptyLength
							else
								0.0
						}
						// Case: Decreased margins
						else
						{
							val usedEndMargin = Stacker.adjustLengths(Vector(targetEndMargin, targetStartMargin), emptyLength).head
							emptyLength - usedEndMargin
						}
					}
					else
					{
						// Case: Enough space available
						if (totalTargetMargin <= emptyLength)
							targetStartMargin.optimal
						// Case: Going out of bounds
						else if (emptyLength <= 0)
							0.0
						// Case: Decreased margins
						else
						{
							val usedStartMargin = Stacker.adjustLengths(Vector(targetStartMargin, targetEndMargin), emptyLength).head
							usedStartMargin
						}
					}
				case None =>
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
		val totalAreaLength = within.size.along(axis)
		val withinStart = within.minAlong(axis)
		val withinEnd = withinStart + totalAreaLength
		
		val areaBefore = (referenceAreaStart - withinStart) max 0
		val areaAfter = (withinEnd - referenceAreaEnd) max 0
		// Primary direction is listed first so that it is preferred
		val areas = Vector(Positive -> areaAfter, Negative -> areaBefore)
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
