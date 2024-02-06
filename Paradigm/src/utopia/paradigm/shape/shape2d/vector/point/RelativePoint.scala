package utopia.paradigm.shape.shape2d.vector.point

import utopia.flow.operator.Reversible
import utopia.flow.operator.combine.{Combinable, LinearScalable, Subtractable}
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.enumeration.OriginType
import utopia.paradigm.enumeration.OriginType.{Absolute, Relative}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import scala.language.implicitConversions

object RelativePoint
{
	// ATTRIBUTES   -------------------
	
	/**
	  * (0,0) absolute & relative position
	  */
	val origin = apply(Point.origin, Point.origin)
	
	
	// IMPLICIT -----------------------
	
	implicit def convertToRelative(p: RelativePoint): Point = p.relative
	
	
	// OTHER    -----------------------
	
	/**
	 * Creates a new relative point
	 * @param relative The relative version of this point
	 * @param absolute The absolute version of this point
	 * @return A new relative point
	 */
	def apply(relative: Point, absolute: Point): RelativePoint = new _RelativePoint(relative, absolute)
	
	/**
	 * Creates a new relative point which also contains a lazily calculated absolute value
	 * @param relative A relative point
	 * @param absolute Function for calculating the absolute point based on the relative point
	 * @return A new relative point
	 */
	def absoluteByRelative(relative: Point)(absolute: Point => Point): RelativePoint =
		new LazyRelativePoint(Lazy.initialized(relative), Lazy { absolute(relative) })
	/**
	 * Creates a new relative point that is based on an absolute value + a lazy computation
	 * @param absolute An absolute point
	 * @param relative A function for calculating the relative point based on the absolute point
	 * @return A new relative point
	 */
	def relativeByAbsolute(absolute: Point)(relative: Point => Point): RelativePoint =
		new LazyRelativePoint(Lazy { relative(absolute) }, Lazy.initialized(absolute))
	
	/**
	 * @param relative Relative point (lazy)
	 * @param absolute Absolute point (lazy)
	 * @return A new lazily initialized relative point
	 */
	def lazily(relative: => Point, absolute: => Point): RelativePoint =
		new LazyRelativePoint(Lazy(relative), Lazy(absolute))
	
	
	// NESTED   -----------------------
	
	private class _RelativePoint(override val relative: Point, override val absolute: Point) extends RelativePoint
	{
		override def map(f: Point => Point): RelativePoint = new _RelativePoint(f(relative), f(absolute))
		override def withRelative(newRelative: Point): RelativePoint = new _RelativePoint(newRelative, absolute)
	}
	
	private class LazyRelativePoint(lazyRelative: Lazy[Point], lazyAbsolute: Lazy[Point]) extends RelativePoint
	{
		// COMPUTED --------------------------
		
		private def current = lazyRelative.current.flatMap { relative =>
			lazyAbsolute.current.map { absolute =>
				new _RelativePoint(relative, absolute)
			}
		}
		
		
		// IMPLEMENTED  ----------------------
		
		override def relative: Point = lazyRelative.value
		override def absolute: Point = lazyAbsolute.value
		
		override def withRelative(newRelative: Point): RelativePoint = lazyAbsolute.current match {
			case Some(absolute) => new _RelativePoint(newRelative, absolute)
			case None => new LazyRelativePoint(Lazy.initialized(newRelative), lazyAbsolute)
		}
		
		override def map(f: Point => Point): RelativePoint = current match {
			case Some(cached) => cached.map(f)
			case None => new LazyRelativePoint(lazyRelative.mapCurrent(f), lazyAbsolute.mapCurrent(f))
		}
		override def mapRelative(f: Point => Point): RelativePoint = current match {
			case Some(cached) => cached.mapRelative(f)
			case None => new LazyRelativePoint(lazyRelative.mapCurrent(f), lazyAbsolute)
		}
	}
}

/**
 * Represents a point that is relative to another point.
 * Contains both the relative and the absolute values.
 * @author Mikko Hilpinen
 * @since 20.2.2023, v1.2.1
 */
trait RelativePoint
	extends EqualsBy with Combinable[HasDoubleDimensions, RelativePoint]
		with Subtractable[HasDoubleDimensions, RelativePoint] with Reversible[RelativePoint]
		with LinearScalable[RelativePoint]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return This point as a relative point
	 */
	def relative: Point
	/**
	 * @return This point as an absolute point
	 */
	def absolute: Point
	
	/**
	  * Changes this point's relative representation, without affecting its absolute position
	  * @param newRelative New relative location
	  * @return
	  */
	def withRelative(newRelative: Point): RelativePoint
	
	/**
	 * Modifies both the relative and the absolute value of this point
	 * @param f A mapping function
	 * @return A mapped copy of this point
	 */
	def map(f: Point => Point): RelativePoint
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return The absolute point to which this point is relative to
	 */
	def anchor = absolute - relative
	
	
	// IMPLEMENTED  --------------------
	
	override def self: RelativePoint = this
	
	override protected def equalsProperties = Vector(relative, absolute)
	
	override def +(other: HasDoubleDimensions): RelativePoint = map { _ + other }
	override def -(other: HasDoubleDimensions): RelativePoint = map { _ - other }
	override def *(mod: Double): RelativePoint = map { _ * mod }
	
	
	// OTHER    ------------------------
	
	/**
	  * @param originType Targeted reference context
	  * @return Either the relative or absolute coordinate matching this point
	  */
	def apply(originType: OriginType) = originType match {
		case Relative => relative
		case Absolute => absolute
	}
	
	/**
	  * Modifies the relative value of this point
	  * @param f A mapping function for the relative point
	  * @return A mapped copy of this point
	  */
	def mapRelative(f: Point => Point): RelativePoint = withRelative(f(relative))
	
	def *(mod: HasDoubleDimensions) = map { _ * mod }
	
	/**
	  * @param newAnchor A new anchor position
	  * @param anchorType Whether the specified anchor is in the relative or the absolute coordinate system.
	  *                   If relative, the point should be relative to this point's anchor.
	  * @return A copy of this point that is relative to the specified anchor instead of the current anchor
	  */
	def relativeTo(newAnchor: Point, anchorType: OriginType): RelativePoint = anchorType match {
		case Absolute => withRelative(absolute - newAnchor)
		case Relative => mapRelative { _ - newAnchor }
	}
}
