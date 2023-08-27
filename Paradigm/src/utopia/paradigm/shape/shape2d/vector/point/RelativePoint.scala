package utopia.paradigm.shape.shape2d.vector.point

import utopia.flow.operator.{Combinable, EqualsBy, LinearScalable, Reversible}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector

import scala.language.implicitConversions

object RelativePoint
{
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
		new ViewRelativePoint(View.fixed(relative), Lazy { absolute(relative) })
	/**
	 * Creates a new relative point that is based on an absolute value + a lazy computation
	 * @param absolute An absolute point
	 * @param relative A function for calculating the relative point based on the absolute point
	 * @return A new relative point
	 */
	def relativeByAbsolute(absolute: Point)(relative: Point => Point): RelativePoint =
		new ViewRelativePoint(Lazy { relative(absolute) }, View.fixed(absolute))
	
	/**
	 * @param relative Relative point (lazy)
	 * @param absolute Absolute point (lazy)
	 * @return A new lazily initialized relative point
	 */
	def lazily(relative: => Point, absolute: => Point): RelativePoint =
		new ViewRelativePoint(Lazy(relative), Lazy(absolute))
	
	
	// NESTED   -----------------------
	
	private class _RelativePoint(override val relative: Point, override val absolute: Point) extends RelativePoint
	{
		override def map(f: Point => Point): RelativePoint = new _RelativePoint(f(relative), f(absolute))
		override def mapRelative(f: Point => Point): RelativePoint = new _RelativePoint(f(relative), absolute)
		
		override def relativeTo(newAnchor: Point): RelativePoint =
			new _RelativePoint(relative + (newAnchor - anchor), absolute)
	}
	
	private class ViewRelativePoint(relativeView: View[Point], absoluteView: View[Point]) extends RelativePoint
	{
		override def relative: Point = relativeView.value
		override def absolute: Point = absoluteView.value
		
		override def map(f: Point => Point): RelativePoint =
			new ViewRelativePoint(Lazy { f(relative) }, Lazy { f(absolute) })
		override def mapRelative(f: Point => Point): RelativePoint =
			new ViewRelativePoint(Lazy { f(relative) }, absoluteView)
		
		override def relativeTo(newAnchor: Point): RelativePoint =
			RelativePoint(relative + (newAnchor - anchor), absolute)
	}
}

/**
 * Represents a point that is relative to another point.
 * Contains both the relative and the absolute values.
 * @author Mikko Hilpinen
 * @since 20.2.2023, v1.2.1
 */
trait RelativePoint
	extends EqualsBy with Combinable[HasDoubleDimensions, RelativePoint] with Reversible[RelativePoint]
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
	 * Modifies both the relative and the absolute value of this point
	 * @param f A mapping function
	 * @return A mapped copy of this point
	 */
	def map(f: Point => Point): RelativePoint
	/**
	 * Modifies the relative value of this point
	 * @param f A mapping function for the relative point
	 * @return A mapped copy of this point
	 */
	def mapRelative(f: Point => Point): RelativePoint
	
	/**
	 * @param newAnchor A new anchor position in an absolute coordinate system
	 * @return A copy of this point that is relative to the specified anchor instead of the current anchor
	 */
	def relativeTo(newAnchor: Point): RelativePoint
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return The point this point is relative to
	 */
	def anchor: Point = absolute - relative
	
	
	// IMPLEMENTED  --------------------
	
	override def self: RelativePoint = this
	
	override protected def equalsProperties = Vector(relative, absolute)
	
	override def +(other: HasDoubleDimensions): RelativePoint = map { _ + other }
	
	override def *(mod: Double): RelativePoint = map { _ * mod }
	
	
	// OTHER    ------------------------
	
	def *(mod: DoubleVector) = map { _ * mod }
}
