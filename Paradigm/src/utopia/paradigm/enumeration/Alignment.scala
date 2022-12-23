package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.paradigm.enumeration
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.LinearAlignment.{Close, Far, Middle}
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

import javax.swing.SwingConstants
import scala.collection.immutable.HashMap

/**
  * Alignments are used for specifying content position when there's additional room
  * @author Mikko Hilpinen
  * @since Genesis 22.4.2019
  */
sealed trait Alignment extends HasDimensions[LinearAlignment]
{
	// ABSTRACT	----------------
	
	/**
	  * @return The horizontal component of this alignment (where the positive direction is right)
	  */
	def horizontal: LinearAlignment
	/**
	  * @return The vertical component of this alignment (where the positive direction is down)
	  */
	def vertical: LinearAlignment
	
	/**
	  * @return The opposite for this alignment, if there is one
	  */
	def opposite: Alignment
	
	/**
	  * @return Swing representation(s) of this alignment, may be horizontal and/or vertical
	  */
	def swingComponents: Map[Axis2D, Int]
	
	
	// COMPUTED	----------------
	
	/**
	  * @return Whether this alignment moves items to the top
	  */
	def isTop = vertical == Close
	/**
	  * @return Whether this alignment moves items to the bottom
	  */
	def isBottom = vertical == Far
	/**
	  * @return Whether this alignment moves items to the left
	  */
	def isLeft = horizontal == Close
	/**
	  * @return Whether this alignment moves items to the right
	  */
	def isRight = horizontal == Far
	
	/**
	  * @return The axes supported for this alignment
	  */
	def affectedAxes: Set[Axis2D] = Axis2D.values.iterator.filter { apply(_).nonZero }.toSet
	
	/**
	  * @return Whether this alignment moves items along the horizontal axis (X)
	  */
	def affectsHorizontal = affects(X)
	/**
	  * @return Whether this alignment moves items along the vertical axis (Y)
	  */
	def affectsVertical = affects(Y)
	/**
	  * @return Whether this alignment moves item along the horizontal axis (X) only
	  */
	def affectsHorizontalOnly = affectsHorizontal && !affectsVertical
	/**
	  * @return Whether this alignment moves items along the vertical axis (Y) only
	  */
	def affectsVerticalOnly = affectsVertical && !affectsHorizontal
	
	/**
	  * @return Directions this alignment will try to move contents
	  */
	def directions: Vector[Direction2D] = Axis2D.values.flatMap { axis => apply(axis).direction.map { axis(_) } }
	
	/**
	  * @return The direction this alignment will move the items horizontally. None if this alignment doesn't
	  *         specify a direction (centered)
	  */
	def horizontalDirection: Option[HorizontalDirection] = horizontal.direction.map { enumeration.HorizontalDirection(_) }
	/**
	  * @return The direction this alignment will move the items vertically. None if this alignment doesn't
	  *         specify a direction (centered)
	  */
	def verticalDirection: Option[VerticalDirection] = vertical.direction.map { enumeration.VerticalDirection(_) }
	
	/**
	  * @return A copy of this alignment that doesn't move items horizontally
	  */
	def onlyVertical = withHorizontal(Middle)
	/**
	  * @return A copy of this alignment that doesn't move items vertically
	  */
	def onlyHorizontal = withVertical(Middle)
	
	/**
	  * @return A copy of this alignment that moves items to the top, vertically
	  */
	def toTop = withVertical(Close)
	/**
	  * @return A copy of this alignment that moves items to the bottom, vertically
	  */
	def toBottom = withVertical(Far)
	/**
	  * @return A copy of this alignment that moves items to the left, horizontally
	  */
	def toLeft = withHorizontal(Close)
	/**
	  * @return A copy of this alignment that moves items to the right, horizontall
	  */
	def toRight = withHorizontal(Far)
	
	
	// IMPLEMENTED  ------------
	
	override def dimensions =
		Dimensions[LinearAlignment](Middle)(Pair(horizontal, vertical))
	
	override def x = horizontal
	override def y = vertical
	
	
	// OTHER	----------------
	
	/**
	  * @param axis An axis
	  * @return Whether this alignment specifies a direction along that axis
	  */
	def affects(axis: Axis) = apply(axis).movesItems
	
	/**
	  * @param direction A direction
	  * @return A copy of this alignment that moves items to that direction
	  */
	def toDirection(direction: Direction2D) = {
		val linearDirection = direction.sign match {
			case Positive => Far
			case Negative => Close
		}
		direction.axis match {
			case X => withHorizontal(linearDirection)
			case Y => withVertical(linearDirection)
		}
	}
	
	/**
	  * @param horizontal New horizontal alignment component
	  * @return A copy of this alignment with the specified horizontal component
	  */
	def withHorizontal(horizontal: LinearAlignment): Alignment = Alignment(horizontal, vertical)
	/**
	  * @param vertical New vertical alignment component
	  * @return A copy of this alignment with the specified vertical component
	  */
	def withVertical(vertical: LinearAlignment): Alignment = Alignment(horizontal, vertical)
	
	/**
	  * @param area An area to position
	  * @param anchor An anchor position, which will be interpreted according to this alignment.
	  *               E.g. for BottomLeft alignment, this anchor point will represent the bottom left corner
	  *               of the resulting area.
	  * @return The top left corner of the resulting area
	  */
	def positionAround(area: Size, anchor: Point = Point.origin) = anchor - origin(area)
	
	/**
	  * Calculates a location matching this alignment within a relative size.
	  * E.g. When used for left alignment, returns the center (Y) left (X) position of the specified size
	  * (assuming (0,0) origin)
	  * @param within A relative area with assumed origin of (0,0)
	  * @return Location of a point matching this alignment within that area (relative position)
	  */
	def origin(within: Size) = Point(dimensions.mergeWith(within, 0.0) { _ origin _ })
	/**
	  * Calculates the location matching this alignment within an area.
	  * E.g. When used for left alignment, returns the center (Y) left (X) position of that area.
	  * @param within An area
	  * @return Location within the specified area that matches this alignment
	  */
	def origin(within: Bounds): Point = Point(dimensions.mergeWith(within, 0.0) { _ origin _ })
	
	/**
	  * Positions a 2D area within a set of 2D boundaries. Won't check whether the area would fit within the boundaries.
	  * @param area Area to position
	  * @param within Boundaries within which the area is positioned
	  * @return The top left coordinate of the area when positioned according to this alignment
	  */
	def position(area: Size, within: Bounds) =
		Point(dimensions.zipWithAxis.map { case (alignment, axis) =>
			within.position(axis) + alignment.position(area(axis), within.size(axis))
		})
	/**
	  * Positions a 2D area within another 2D area. Won't check whether the area would fit within the boundaries.
	  * @param area Area to position
	  * @param within Size of the area within which the area is positioned
	  * @return The top left coordinate of the area when positioned according to this alignment,
	  *         relative to the containment area top left position.
	  */
	def position(area: Size, within: Size): Point =
		Point(dimensions.zipWithAxis.map { case (alignment, axis) =>
			alignment.position(area(axis), within(axis))
		})
	
	/**
	  * Positions a 2D area within a set of 2D boundaries. Downscales the area if it wouldn't otherwise fit into the
	  * target boundaries.
	  * @param area Area to position
	  * @param within Boundaries within which the area is positioned
	  * @param preserveShape When downscaling, whether to preserve the ratio between area.width and area.height
	  *                      (default = true)
	  * @return Positioned and possible scaled copy of the specified area
	  */
	def positionToFit(area: Size, within: Bounds, preserveShape: Boolean = true) = {
		// Fits the area within boundaries
		val fittedArea = {
			if (preserveShape) {
				val scaling = (within.size / area).minDimension
				if (scaling < 1.0)
					area * scaling
				else
					area
			}
			else {
				val scaling = (within.size / area).map { _ min 1.0 }
				area * scaling
			}
		}
		// Positions the area, returns as boundaries
		Bounds(position(fittedArea, within), fittedArea)
	}
}

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
		
		/**
		  * @return The main component of this alignment
		  */
		def linear: LinearAlignment
		
		
		// COMPUTED	---------------------
		
		/**
		  * @return Axis used by this alignment
		  */
		def axis = direction.axis
		
		/**
		  * @return The directional sign of this alignment
		  */
		def sign = direction.sign
		
		
		// IMPLEMENTED	-----------------
		
		override def affectedAxes = Set(axis)
		
		override def directions = Vector(direction)
	}
	
	/**
	  * Common trait for alignments that only represent a horizontal direction
	  */
	sealed trait HorizontalAlignment extends SingleDirectionAlignment[HorizontalDirection]
	{
		// IMPLEMENTED	-------------------
		
		override def horizontal = linear
		override def vertical = Middle
		
		override def horizontalDirection = Some(direction)
		override def verticalDirection = None
	}
	
	/**
	  * Common trait for alignments that only represent a vertical direction
	  */
	sealed trait VerticalAlignment extends SingleDirectionAlignment[VerticalDirection]
	{
		// IMPLEMENTED	-------------------
		
		override def horizontal = Middle
		override def vertical = linear
		
		override def horizontalDirection = None
		override def verticalDirection = Some(direction)
	}
	
	/**
	  * In left alignment, content is leading
	  */
	case object Left extends HorizontalAlignment
	{
		override def toString = "Left"
		
		override def linear = Close
		override def direction = Direction2D.Left
		
		override def opposite = Right
		
		override def swingComponents = HashMap(X -> SwingConstants.LEADING)
	}
	
	/**
	  * In right alignment, content is trailing
	  */
	case object Right extends HorizontalAlignment
	{
		override def toString = "Right"
		
		override def linear = Far
		override def direction = Direction2D.Right
		
		override def opposite = Left
		
		override def swingComponents = HashMap(X -> SwingConstants.TRAILING)
	}
	
	/**
	  * In top alignment, content is positioned at the the top of a component
	  */
	case object Top extends VerticalAlignment
	{
		override def toString = "Top"
		
		override def linear = Close
		override def direction = Direction2D.Up
		
		override def opposite = Bottom
		
		override def swingComponents = HashMap(Y -> SwingConstants.TOP)
	}
	
	/**
	  * In bottom alignment, content is positioned at the bottom of a component
	  */
	case object Bottom extends VerticalAlignment
	{
		override def toString = "Bottom"
		
		override def linear = Far
		override def direction = Direction2D.Down
		
		override def opposite = Top
		
		override def swingComponents = HashMap(Y -> SwingConstants.BOTTOM)
	}
	
	/**
	  * In center alignment, content is positioned at the center of a component
	  */
	case object Center extends Alignment
	{
		override def toString = "Center"
		
		override def horizontal = Middle
		override def vertical = Middle
		
		override def opposite = this
		
		override def swingComponents =
			HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.CENTER)
		
		override def directions = Vector[Direction2D]()
		override def horizontalDirection = None
		override def verticalDirection = None
	}
	
	case object TopLeft extends Alignment
	{
		override def toString = "Top Left"
		
		override def horizontal = Close
		override def vertical = Close
		
		override def opposite = BottomRight
		
		override def swingComponents = HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.TOP)
		
		override def directions = Vector(Direction2D.Up, Direction2D.Left)
		override def horizontalDirection = Some(Direction2D.Left)
		override def verticalDirection = Some(Direction2D.Up)
	}
	
	case object TopRight extends Alignment
	{
		override def toString = "Top Right"
		
		override def horizontal = Far
		override def vertical = Close
		
		override def opposite = BottomLeft
		
		override def swingComponents =
			HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.TOP)
		
		override def directions = Vector(Direction2D.Up, Direction2D.Right)
		override def horizontalDirection = Some(Direction2D.Right)
		override def verticalDirection = Some(Direction2D.Up)
	}
	
	case object BottomLeft extends Alignment
	{
		override def toString = "Bottom Left"
		
		override def horizontal = Close
		override def vertical = Far
		
		override def opposite = TopRight
		
		override def swingComponents =
			HashMap(X -> SwingConstants.LEADING, Y -> SwingConstants.BOTTOM)
		
		override def directions = Vector(Direction2D.Down, Direction2D.Left)
		override def horizontalDirection = Some(Direction2D.Left)
		override def verticalDirection = Some(Direction2D.Down)
	}
	
	case object BottomRight extends Alignment
	{
		override def toString = "Bottom Right"
		
		override def horizontal = Far
		override def vertical = Far
		
		override def opposite = TopLeft
		
		override def swingComponents =
			HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.BOTTOM)
		
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
	  * @param linear A (horizontal) linear alignment
	  * @return An alignment matching that linear alignment (no vertical component)
	  */
	def horizontal(linear: LinearAlignment) = linear match {
		case Close => Alignment.Left
		case Middle => Center
		case Far => Alignment.Right
	}
	/**
	  * @param linear A (vertical) linear alignment
	  * @return An alignment matching that linear alignment (no horizontal component)
	  */
	def vertical(linear: LinearAlignment) = linear match {
		case Close => Top
		case Middle => Center
		case Far => Bottom
	}
	/**
	  * @param horizontal A horizontal alignment component
	  * @param vertical A vertical alignment component
	  * @return An alignment combined from those two components
	  */
	def apply(horizontal: LinearAlignment, vertical: LinearAlignment) = horizontal match {
		case Close =>
			vertical match {
				case Close => TopLeft
				case Middle => Left
				case Far => BottomLeft
			}
		case Middle =>
			vertical match {
				case Close => Top
				case Middle => Center
				case Far => Bottom
			}
		case Far =>
			vertical match {
				case Close => TopRight
				case Middle => Right
				case Far => BottomRight
			}
	}
	
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
	def forHorizontalSwingAlignment(alignment: Int): Option[Alignment] =
		horizontal.find { _.swingComponents.get(X).contains(alignment) }
	/**
	  * @param alignment Searched swing alignment
	  * @return A vertical alignment matching the specified swing alignment. None if no alignment matched.
	  */
	def forVerticalSwingAlignment(alignment: Int): Option[Alignment] =
		vertical.find { _.swingComponents.get(Y).contains(alignment) }
	
	/**
	  * @param alignment Swing constant for alignment
	  * @return An alignment matchin the swing constant. None if no alignment matches the constant.
	  */
	def forSwingAlignment(alignment: Int): Option[Alignment] =
		forHorizontalSwingAlignment(alignment) orElse forVerticalSwingAlignment(alignment)
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