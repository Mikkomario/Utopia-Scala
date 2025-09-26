package utopia.firmament.model

import utopia.flow.collection.immutable.Pair
import utopia.flow.util.Mutate
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.shape.shape2d.insets._
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.Dimensions

import javax.swing.BorderFactory
import javax.swing.border.{EmptyBorder, MatteBorder}

object Border
{
	// ATTRIBUTES	-------------------------
	
	/**
	 * A factory for constructing empty / non-colored borders
	 */
	lazy val transparent = BorderFactory(None)
	/**
	  * A border with no size and no color
	  */
	lazy val zero = transparent(Insets.zero)
	
	
	// COMPUTED -----------------------------
	
	@deprecated("Renamed to .transparent", "v1.6")
	def empty = transparent
	
	
	// OTHER	-----------------------------
	
	/**
	 * @param color Color of these borders
	 * @return A factory for finalizing these borders
	 */
	def apply(color: Color) = BorderFactory(Some(color))
	/**
	 * @param color Color of these borders. None if transparent.
	 * @return A factory for finalizing these borders.
	 */
	def apply(color: Option[Color]) = BorderFactory(color)
	
	/**
	 * @param color Color of these borders
	 * @param width Width of these borders
	 * @return A symmetric set of borders with the specified color and width
	 */
	def apply(color: Color, width: Double): Border = apply(color)(width)
	/**
	  * @param width Border width (applied to all sides)
	  * @param color Border color
	  * @return A new symmetric set of borders
	  */
	def apply(width: Double, color: Color): Border = apply(color, width)
	
	/**
	  * Creates a new border
	  * @param insets Border insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).apply(Insets) instead", "v1.6")
	def apply(insets: Insets, color: Color): Border = Border(insets, Some(color))
	/**
	  * Creates a new border
	  * @param left Left side width
	  * @param right Right side width
	  * @param top Top side width
	  * @param bottom Bottom side width
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).apply(Double, Double, Double, Double) instead", "v1.6")
	def apply(left: Double, right: Double, top: Double, bottom: Double, color: Color): Border =
		apply(Insets(left, right, top, bottom), color)
	/**
	  * Creates a new border
	  * @param sizes Border insets
	  * @param color Border color
	  * @param inner The border inside this one
	  * @return A new border with another border inside
	  */
	@deprecated("Please use .apply(Color).apply(Insets).withInner(Border) instead", "v1.6")
	def apply(sizes: Insets, color: Color, inner: Border): Border = Border(sizes, Some(color), Some(inner))
	
	/**
	  * @param direction Targeted direction
	  * @param width Width of the border on that direction
	  * @param color Border color
	  * @return A new border that only affects the targeted direction
	  */
	@deprecated("Please use .apply(Color).apply(Direction2D, Double) instead", "v1.6")
	def towards(direction: Direction2D, width: Double, color: Color) = Border(Insets.apply(direction, width), color)
	
	/**
	  * @param height Height of this border
	  * @param color Border color
	  * @return A border that only affects the top of a component
	  */
	@deprecated("Please use .apply(Color).top instead", "v1.6")
	def top(height: Double, color: Color) = towards(Up, height, color)
	/**
	  * @param height Height of this border
	  * @param color Border color
	  * @return A border that only affects the bottom of a component
	  */
	@deprecated("Please use .apply(Color).bottom instead", "v1.6")
	def bottom(height: Double, color: Color) = towards(Down, height, color)
	/**
	  * @param width Width of this border
	  * @param color Border color
	  * @return A border that only affects the left side of a component
	  */
	@deprecated("Please use .apply(Color).left instead", "v1.6")
	def left(width: Double, color: Color) = towards(Direction2D.Left, width, color)
	/**
	  * @param width Width of this border
	  * @param color Border color
	  * @return A border that only affects the right side of a component
	  */
	@deprecated("Please use .apply(Color).right instead", "v1.6")
	def right(width: Double, color: Color) = towards(Direction2D.Right, width, color)
	
	/**
	  * Creates a symmetric border
	  * @param side Length of each side
	  * @param color Color used in this border
	  * @return A symmetric border
	  */
	@deprecated("Please use .apply(Double, Color) instead", "v1.6")
	def symmetric(side: Double, color: Color) = Border(Insets.symmetric(side), color)
	/**
	  * Creates a symmetric border
	  * @param hBorder Left & right insets
	  * @param vBorder Top & bottom insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).symmetric(Double, Double) instead", "v1.6")
	def symmetric(hBorder: Double, vBorder: Double, color: Color) = Border(Insets.symmetric(hBorder, vBorder), color)
	/**
	  * Creates a symmetric border
	  * @param insets Total border insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).apply(Size) instead", "v1.6")
	def symmetric(insets: Size, color: Color): Border = apply(color).apply(insets)
	
	/**
	  * Creates a horizontal border
	  * @param left Left side insets
	  * @param right Right side insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).horizontal(Double, Double) instead", "v1.6")
	def horizontal(left: Double, right: Double, color: Color) = Border(Insets(left, right, 0, 0), color)
	/**
	  * Creates a horizontal border
	  * @param hBorder left & right insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).horizontal(Double) instead", "v1.6")
	def horizontal(hBorder: Double, color: Color): Border = horizontal(hBorder, hBorder, color)
	/**
	  * Creates a vertical border
	  * @param top Top insets
	  * @param bottom Bottom insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).vertical(Double, Double) instead", "v1.6")
	def vertical(top: Double, bottom: Double, color: Color) = Border(Insets(0, 0, top, bottom), color)
	/**
	  * Creates a vertical border
	  * @param vBorder Top & bottom insets
	  * @param color Border color
	  * @return A new border
	  */
	@deprecated("Please use .apply(Color).vertical(Double) instead", "v1.6")
	def vertical(vBorder: Double, color: Color): Border = vertical(vBorder, vBorder, color)
	
	/**
	  * Creates a new border that looks like it was raised
	  * @param w The width of each side
	  * @param baseColor Base color (actual colors will vary based on variance mod)
	  * @param intensity The intensity of color change applied where 0 is no change and 1 is the default change
	  * @return A new border
	  */
	def raised(w: Double, baseColor: Color, intensity: Double = 1.0) = {
		val dark = apply(baseColor.darkenedBy(intensity))(0, w, 0, w)
		apply(dark.insets.opposite, Some(baseColor.lightenedBy(intensity)), Some(dark))
	}
	/**
	  * Creates a new border that looks like it was lowered
	  * @param w The width of each side
	  * @param baseColor Base color (actual colors will vary based on variance mod)
	  * @param intensity The intensity of color change applied where 0 is no change and 1 is the default change
	  * @return A new border
	  */
	def lowered(w: Double, baseColor: Color, intensity: Double = 1.0) =
		raised(w, baseColor, intensity).opposite
		
	
	// NESTED   ----------------------------
	
	case class BorderFactory(color: Option[Color]) extends SidesFactory[Double, Border]
	{
		// IMPLEMENTED  --------------------
		
		override def withSides(sides: Map[Direction2D, Double]): Border = apply(Insets(sides))
		
		
		// OTHER    ------------------------
		
		/**
		 * @param sides Sides that specify the width of these borders
		 * @return A border with the specified widths
		 */
		def apply(sides: Insets) = Border(sides, color)
		/**
		 * @param sizes The horizontal & vertical borders
		 * @return A symmetric set of borders with the specified dimensions
		 */
		def apply(sizes: Size): Border = apply(Insets.symmetric(sizes))
	}
}

/**
  * Borders are placed inside component bounds. They have a size and a color
  * @author Mikko Hilpinen
  * @since 29.4.2019, Reflection v1+
  */
case class Border(insets: Insets, color: Option[Color], inner: Option[Border] = None)
	extends Sides[Double] with InsetsLike[Border]
{
	override lazy val total: Size = inner match {
		case Some(inner) => inner.total + insets.total
		case None => insets.total
	}
	/**
	 * @return The total insets of this border
	 */
	lazy val totalInsets: Insets = inner match {
		case Some(inner) => inner.totalInsets + insets
		case None => insets
	}
	
	// COMPUTED	-------------------
	
	/**
	 * @return Copy of this border without any coloring
	 */
	def transparent = copy(color = None)
	
	/**
	  * @return An awt representation of this border
	  */
	def toAwt: javax.swing.border.Border = {
		val myPart = color.map { c => new MatteBorder(insets.toAwt, c.toAwt) } getOrElse new EmptyBorder(insets.toAwt)
		inner.map { i => BorderFactory.createCompoundBorder(myPart, i.toAwt) } getOrElse myPart
	}
	
	
	// IMPLEMENTED  ---------------
	
	override def self: Border = this
	override def sides: Map[Direction2D, Double] = insets.sides
	
	override def positive: Border = _map { _.positive }
	override def opposite: Border = _map { _.opposite }
	override def round: Border = _map { _.round }
	
	override protected def withSides(sides: Map[Direction2D, Double]): Border = copy(insets = Insets(sides))
	
	override def mapWithSide(f: (Direction2D, Double) => Double): Border = _map { _.mapWithSide(f) }
	override def mapDefined(f: Double => Double): Border = _map { _.mapDefined(f) }
	override def flatMapDefined(f: Double => Option[Double]): Border = _map { _.flatMapDefined(f) }
	override def mapDimensions(f: Dimensions[Pair[Double]] => Dimensions[Pair[Double]]): Border =
		_map { _.mapDimensions(f) }
	
	override def filter(f: (Direction2D, Double) => Boolean): Border = _map { _.filter(f) }
	
	override def mirroredAlong(axis: Axis2D): Border = _map { _.mirroredAlong(axis) }
	
	override def +(other: HasSides[Double]): Border = other match {
		case b: Border => inside(b)
		case o => super.+(o)
	}
	override def ++(sides: HasSides[Double]): Border = sides match {
		case b: Border => inside(b)
		case o => super.++(o)
	}
	
	
	// OTHER	------------------
	
	/**
	 * Combines this border with another
	 * @param another Another border
	 * @return A copy of this border with another border inside
	 */
	def +(another: Border): Border = withInner(another)
	/**
	 * Removes a border from this border
	 * @param another Another border
	 * @return A copy of this border without the provided border inside
	 */
	@deprecated("Deprecated for removal", "v1.8")
	def -(another: Border): Border = inner match {
		case Some(inner) =>
			if (inner == another)
				copy(inner = None)
			else
				copy(inner = Some(inner - another))
		case None => this
	}
	
	/**
	 * @param color Color assigned to these borders
	 * @return A copy of these borders with the specified color
	 */
	def withColor(color: Color) = copy(color = Some(color))
	/**
	  * Maps the color of this border
	  * @param f A mapping function
	  * @return A copy of this border with mapped color
	  */
	def mapColor(f: Mutate[Color]): Border = copy(color = color.map(f), inner = inner.map { _.mapColor(f) })
	
	/**
	 * @param inner A border to place inside this one
	 * @return A copy of this border with the specified borders inside it
	 */
	def withInner(inner: Border): Border = this.inner match {
		case Some(existing) => copy(inner = Some(existing.withInner(inner)))
		case None => copy(inner = Some(inner))
	}
	/**
	  * Creates a new border with this border inside it
	  * @param another Another border
	  * @return A copy of the other border with this one inside it
	  */
	def inside(another: Border) = another.withInner(this)
	
	private def _map(f: Mutate[Insets]): Border = copy(insets = f(insets), inner = inner.map { _._map(f) })
}
