package utopia.paradigm.enumeration

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.paradigm.enumeration
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.LinearAlignment.{Close, Far, Middle}
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.paradigm.shape.template.{Dimensional, Dimensions, DimensionsWrapperFactory, HasDimensions}

import javax.swing.SwingConstants
import scala.collection.immutable.HashMap

/**
  * Alignments are used for specifying content position when there's additional room
  * @author Mikko Hilpinen
  * @since Genesis 22.4.2019
  */
sealed trait Alignment extends Dimensional[LinearAlignment, Alignment]
{
	// ABSTRACT	----------------
	
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
	  * @return The horizontal component of this alignment (where the positive direction is right)
	  */
	def horizontal: LinearAlignment = x
	/**
	  * @return The vertical component of this alignment (where the positive direction is down)
	  */
	def vertical: LinearAlignment = y
	
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
	def directions = Axis2D.values.flatMap { axis => apply(axis).direction.map { axis(_) } }
	
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
	
	override def self: Alignment = this
	
	override def withDimensions(newDimensions: Dimensions[LinearAlignment]): Alignment = Alignment(newDimensions)
	
	
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
		Bounds.fromFunction2D { axis => apply(axis).position(area(axis), within(axis)) }
	/**
	  * Positions a 2D area within another 2D area. Won't check whether the area would fit within the boundaries.
	  * @param area Area to position
	  * @param within Size of the area within which the area is positioned
	  * @return The top left coordinate of the area when positioned according to this alignment,
	  *         relative to the containment area top left position.
	  */
	def position(area: Size, within: Size): Point =
		Point.fromFunction2D { axis => apply(axis).position(area(axis), within(axis)) }
	/**
	  * Positions a 2D area within a set of 2D boundaries. Downscales the area if it wouldn't otherwise fit into the
	  * target boundaries.
	  * @param area Area to position
	  * @param within Boundaries within which the area is positioned
	  * @param preserveShape When downscaling, whether to preserve the ratio between area.width and area.height
	  *                      (default = true)
	  * @return Positioned and possible scaled copy of the specified area
	  */
	def fit(area: Size, within: Bounds, preserveShape: Boolean = true) = {
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
		position(fittedArea, within)
	}
	@deprecated("Renamed to .fit(...)", "v1.3")
	def positionToFit(area: Size, within: Bounds, preserveShape: Boolean = true) = fit(area, within, preserveShape)
	/**
	  * Positions an area relative to another area so that the relationship between the two resulting areas will
	  * match this alignment.
	  * E.g. When called for Left, will place the resulting area left to the 'to' area.
	  *
	  * For bi-directional alignments such as TopRight, will place the resulting area so that the two areas won't
	  * share any edges (i.e. are placed diagonally).
	  *
	  * @param area An area to place
	  * @param to The area the 'area' will be placed next to
	  * @param margin Margin to place between the two areas
	  *
	  * @return An area with the specified size 'area' that lies next to 'to'
	  *         (when called for Center, the resulting area will lie over the center of 'to')
	  */
	def positionRelativeTo(area: Size, to: Bounds, margin: Double = 0.0) = Bounds.fromFunction2D { axis =>
		apply(axis).positionRelativeTo(area(axis), to(axis), margin)
	}
	/**
	  * Positions an area relative to another area so that the relationship between the two resulting areas will
	  * match this alignment, but also fit within the specified set of bounds.
	  * Will not alter the size of the specified area, however. If the specified area is too large to fit, it will
	  * completely cover the 'within' area and expand to outside as well.
	  *
	  * @param area The area to position
	  * @param to The area relative to which the 'area' is positioned
	  * @param within The area within which the resulting area must reside (as much as possible)
	  * @param margin Margin placed between 'area' and 'to', when there is enough space.
	  *               Ignored for Center alignment.
	  *               Default = 0 = no margin
	  * @param swapToFit Whether this alignment may be swapped to its opposite in case that would yield better results.
	  *                  Better in this case means less shifting in order to fulfill the 'within' requirement.
	  *                  This means less overlap between the two resulting areas.
	  *                  This parameter has no meaning when called for Center.
	  *                  Default = false
	  *
	  * @return An area that lies within 'within' as much as possible and is located relative to 'to',
	  *         according to this alignment.
	  */
	def positionRelativeToWithin(area: Size, to: Bounds, within: Bounds, margin: Double = 0.0,
	                             swapToFit: Boolean = false) =
	{
		// Moves first without considering the 'within' limits
		val primary = positionRelativeTo(area, to, margin)
		// Case: Fits by default => Returns
		if (within.contains(primary))
			primary
		// Case: Doesn't fit and swapping is enabled => Attempts swapping to the opposite alignment
		else if (swapToFit) {
			val secondary = opposite.positionRelativeTo(area, to, margin)
			// Case: Swapped fits => Returns that
			if (within.contains(secondary))
				secondary
			// Case: Neither of the modes fit => Returns the one that has less overlap (shifted)
			else {
				val better = Vector(primary, secondary).minBy { area =>
					area.overlapWith(within) match {
						case Some(overlap) => overlap.area
						case None => 0.0
					}
				}
				better.shiftedInto(within)
			}
		}
		// Case: Swapping is not enabled => Shifts the area so that it will lie within the target area
		else
			primary.shiftedInto(within)
	}
	/**
	  * Positions an area relative to another area so that the relationship between the two resulting areas will
	  * match this alignment, but also fit within the specified set of bounds.
	  * If the specified area doesn't fit the specified 'within' bounds, the resulting area will be smaller than
	  * the specified area. I.e. the resulting area will never expand outside the 'within' area.
	  *
	  * @param area      The area to position
	  * @param to        The area relative to which the 'area' is positioned
	  * @param within    The area within which the resulting area will reside
	  * @param margin    Margin placed between 'area' and 'to', when there is enough space.
	  *                  Ignored for Center alignment.
	  *                  Default = 0 = no margin
	  * @param swapToFit Whether this alignment may be swapped to its opposite in case that would yield better results.
	  *                  Better in this case means less shifting in order to fulfill the 'within' requirement.
	  *                  This means less overlap between the two resulting areas.
	  *                  This parameter has no meaning when called for Center.
	  *                  Default = false.
	  * @param preserveShape Whether the shape (x/y ratio) of the specified 'area' should match that of the resulting
	  *                      area. Setting this to false may give you more space to deal with, but the shape may be
	  *                      different.
	  *                      Default = true = shape is preserved.
	  *
	  * @return An area that lies within 'within' and is located relative to 'to', according to this alignment.
	  */
	def fitRelativeToWithin(area: Size, to: Bounds, within: Bounds, margin: Double = 0.0, swapToFit: Boolean = false,
	                        preserveShape: Boolean = true) =
	{
		// Case: The specified area fits within 'within' => Positions it
		if (area.forAllDimensionsWith(within) { _ <= _.length })
			positionRelativeToWithin(area, to, within, margin, swapToFit)
		// Case: The resulting area will cover the whole 'within' => Skips the positioning
		else if (!preserveShape && area.forAllDimensionsWith(within) { _ >= _.length })
			within
		// Case: The specified area is too large and has to be downscaled => Positions the downscaled version
		else {
			val scaling = (within.size / area).xyPair.min
			positionRelativeToWithin(area * scaling, to, within, margin, swapToFit)
		}
	}
}

object Alignment extends DimensionsWrapperFactory[LinearAlignment, Alignment]
{
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
	
	
	// IMPLEMENTED  -------------------
	
	override def zeroDimension = Middle
	
	override def apply(dimensions: Dimensions[LinearAlignment]): Alignment = _apply(dimensions.x, dimensions.y)
	
	override def from(other: HasDimensions[LinearAlignment]): Alignment = other match {
		case a: Alignment => a
		case _ => _apply(other.x, other.y)
	}
	
	/**
	  * @param horizontal A horizontal alignment component
	  * @param vertical   A vertical alignment component
	  * @return An alignment combined from those two components
	  */
	override def apply(horizontal: LinearAlignment, vertical: LinearAlignment) = _apply(horizontal, vertical)
	
	
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
	  * @param direction Target direction
	  * @return An alignment that places items to that direction
	  */
	def forDirection(direction: Direction2D) = direction match {
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
	def forSwingAlignments(horizontal: Int, vertical: Int): Alignment = {
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
	
	private def _apply(horizontal: LinearAlignment, vertical: LinearAlignment): Alignment = horizontal match {
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
	
	
	// NESTED   ---------------------------------
	
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
		
		override lazy val dimensions: Dimensions[LinearAlignment] = Dimensions[LinearAlignment](Middle)(linear, Middle)
		
		override def horizontalDirection = Some(direction)
		override def verticalDirection = None
	}
	
	/**
	  * Common trait for alignments that only represent a vertical direction
	  */
	sealed trait VerticalAlignment extends SingleDirectionAlignment[VerticalDirection]
	{
		// IMPLEMENTED	-------------------
		
		override lazy val dimensions: Dimensions[LinearAlignment] = Dimensions[LinearAlignment](Middle)(Middle, linear)
		
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
		override lazy val dimensions: Dimensions[LinearAlignment] = Dimensions(Middle).zero2D
		
		override def toString = "Center"
		
		override def opposite = this
		
		override def swingComponents =
			HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.CENTER)
		
		override def directions = Vector[Direction2D]()
		
		override def horizontalDirection = None
		override def verticalDirection = None
	}
	
	case object TopLeft extends Alignment
	{
		override lazy val dimensions = Dimensions[LinearAlignment](Middle)(Close, Close)
		
		override def toString = "Top Left"
		
		override def opposite = BottomRight
		
		override def swingComponents = HashMap(X -> SwingConstants.CENTER, Y -> SwingConstants.TOP)
		
		override def directions = Vector(Direction2D.Up, Direction2D.Left)
		
		override def horizontalDirection = Some(Direction2D.Left)
		
		override def verticalDirection = Some(Direction2D.Up)
	}
	
	case object TopRight extends Alignment
	{
		override lazy val dimensions = Dimensions[LinearAlignment](Middle)(Far, Close)
		
		override def toString = "Top Right"
		
		override def opposite = BottomLeft
		
		override def swingComponents =
			HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.TOP)
		
		override def directions = Vector(Direction2D.Up, Direction2D.Right)
		
		override def horizontalDirection = Some(Direction2D.Right)
		
		override def verticalDirection = Some(Direction2D.Up)
	}
	
	case object BottomLeft extends Alignment
	{
		override lazy val dimensions = Dimensions[LinearAlignment](Middle)(Close, Far)
		
		override def toString = "Bottom Left"
		
		override def opposite = TopRight
		
		override def swingComponents =
			HashMap(X -> SwingConstants.LEADING, Y -> SwingConstants.BOTTOM)
		
		override def directions = Vector(Direction2D.Down, Direction2D.Left)
		
		override def horizontalDirection = Some(Direction2D.Left)
		
		override def verticalDirection = Some(Direction2D.Down)
	}
	
	case object BottomRight extends Alignment
	{
		override lazy val dimensions = Dimensions[LinearAlignment](Middle)(Far, Far)
		
		override def toString = "Bottom Right"
		
		override def opposite = TopLeft
		
		override def swingComponents =
			HashMap(X -> SwingConstants.TRAILING, Y -> SwingConstants.BOTTOM)
		
		override def directions = Vector(Direction2D.Down, Direction2D.Right)
		
		override def horizontalDirection = Some(Direction2D.Right)
		
		override def verticalDirection = Some(Direction2D.Down)
	}
}