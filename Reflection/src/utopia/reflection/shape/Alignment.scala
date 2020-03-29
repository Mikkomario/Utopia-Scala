package utopia.reflection.shape

import javax.swing.SwingConstants
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Direction2D, Point, Size}
import utopia.reflection.container.stack.Stacker

import scala.collection.immutable.HashMap

object Alignment
{
	/**
	  * Common trait for alignments that only represent a single direction
	  */
	sealed trait SingleDirectionAlignment extends Alignment
	{
		// ABSTRACT	---------------------
		
		/**
		  * @return Axis used by this alignment
		  */
		def axis: Axis2D
		
		/**
		  * @return Whether this alignment moves items to the positive (+) direction on the specified axis
		  */
		def isPositiveDirection: Boolean
		
		
		// COMPUTED	---------------------
		
		/**
		  * @return The direction matching this alignment
		  */
		def direction = Direction2D(axis, isPositiveDirection)
		
		
		// IMPLEMENTED	-----------------
		
		override def supportedAxes = Set(axis)
		
		override def directions = Vector(direction)
	}
	
	/**
	  * Common trait for alignments that only represent a horizontal direction
	  */
	sealed trait HorizontalAlignment extends SingleDirectionAlignment
	{
		// IMPLEMENTED	-------------------
		
		override def axis = X
		
		override def vertical = Center
	}
	
	/**
	  * Common trait for alignments that only represent a vertical direction
	  */
	sealed trait VerticalAlignment extends SingleDirectionAlignment
	{
		// IMPLEMENTED	-------------------
		
		override def axis = Y
		
		override def horizontal = Center
	}
	
	/**
	  * In left alignment, content is leading
	  */
	case object Left extends HorizontalAlignment
	{
		override def isPositiveDirection = false
		
		override def opposite = Right
		
		override def swingComponents = HashMap(X -> SwingConstants.LEADING)
		
		override def horizontal = this
	}
	
	/**
	  * In right alignment, content is trailing
	  */
	case object Right extends HorizontalAlignment
	{
		override def isPositiveDirection = true
		
		override def opposite = Left
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING)
		
		override def horizontal = this
	}
	
	/**
	  * In top alignment, content is positioned at the the top of a component
	  */
	case object Top extends VerticalAlignment
	{
		override def isPositiveDirection = false
		
		override def opposite = Bottom
		
		override def swingComponents = HashMap(Y -> SwingConstants.TOP)
		
		override def vertical = this
	}
	
	/**
	  * In bottom alignment, content is positioned at the bottom of a component
	  */
	case object Bottom extends VerticalAlignment
	{
		override def isPositiveDirection = true
		
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
	}
	
	case object TopLeft extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = BottomRight
		
		override def swingComponents = HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.TOP)
		
		override def horizontal = Left
		
		override def vertical = Top
		
		override def directions = Vector(Direction2D.Up, Direction2D.Left)
	}
	
	case object TopRight extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = BottomLeft
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.TOP)
		
		override def horizontal = Right
		
		override def vertical = Top
		
		override def directions = Vector(Direction2D.Up, Direction2D.Right)
	}
	
	case object BottomLeft extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = TopRight
		
		override def swingComponents = HashMap(X -> SwingConstants.LEADING, Y -> SwingConstants.BOTTOM)
		
		override def horizontal = Left
		
		override def vertical = Bottom
		
		override def directions = Vector(Direction2D.Down, Direction2D.Left)
	}
	
	case object BottomRight extends Alignment
	{
		override def supportedAxes = Set(X, Y)
		
		override def opposite = TopLeft
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.BOTTOM)
		
		override def horizontal = Right
		
		override def vertical = Bottom
		
		override def directions = Vector(Direction2D.Down, Direction2D.Right)
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
	
	
	// COMPUTED	----------------
	
	/**
	  * @return Whether this alignment can be used for horizontal axis (X)
	  */
	def isHorizontal = supportedAxes.contains(X)
	
	/**
	  * @return Whether this alignment can be used for vertical axis (Y)
	  */
	def isVertical = supportedAxes.contains(Y)
	
	
	// OTHER	----------------
	
	/**
	  * @param axis Target axis
	  * @return A version of this alignment that can be used for the specified axis
	  */
	def along(axis: Axis2D) = if (supportedAxes.contains(axis)) this else Center
	
	/**
	  * @param axis Target axis
	  * @return Direction determined by this alignment on specified axis. None if this alignment doesn't specify a
	  *         direction along specified axis (centered)
	  */
	def directionAlong(axis: Axis2D) = directions.find { _.axis == axis }
	
	/**
	  * Positions the specified area within a set of bounds so that it follows this alignment
	  * @param areaToPosition Size of the area/element to position (Eg. content size)
	  * @param within The bounds within which the area is positioned (Eg. component bounds)
	  * @param margins Margins used around the positioned content (default = 0 on each side).
	  *                If the content wouldn't fit into the target area with these margins, the margins are decreased.
	  *                The maximum amounts in the margins are ignored.
	  * @param fitWithinBounds Whether the resulting bounds should be fit within the 'within' bounds (default = true).
	  *                        Only used when the 'areaToPosition' doesn't naturally fit into the specified bounds.
	  * @return A set of bounds for the area to position that follows this alignment
	  */
	def position(areaToPosition: Size, within: Bounds, margins: StackInsets = StackInsets.any, fitWithinBounds: Boolean = true) =
	{
		val topLeft = Point.calculateWith { axis =>
			val (startMargin, endMargin) = margins.sidesAlong(axis)
			positionWithDirection(areaToPosition.along(axis), within.size.along(axis), startMargin, endMargin,
				directionAlong(axis), !fitWithinBounds)
		}
		// May need to shrink the resulting bounds
		if (fitWithinBounds)
			Bounds(topLeft, areaToPosition min within.size)
		else
			Bounds(topLeft, areaToPosition)
	}
	
	private def positionWithDirection(length: Double, withinLength: Double, targetStartMargin: StackLength,
									  targetEndMargin: StackLength, direction: Option[Direction2D], allowStartBelowZero: Boolean) =
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
					if (definedDirection.isPositiveDirection)
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
}
