package utopia.firmament.model

import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.size.Size

import javax.swing.BorderFactory
import javax.swing.border.{EmptyBorder, MatteBorder}

object Border
{
	// ATTRIBUTES	-------------------------
	
	/**
	  * A border with no size and no color
	  */
	val zero = empty(Insets.zero)
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param width Border width (applied to all sides)
	  * @param color Border color
	  * @return A new symmetric set of borders
	  */
	def apply(width: Double, color: Color): Border = symmetric(width, color)
	/**
	  * Creates a new border
	  * @param insets Border insets
	  * @param color Border color
	  * @return A new border
	  */
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
	def apply(left: Double, right: Double, top: Double, bottom: Double, color: Color): Border = apply(Insets(left, right, top, bottom), color)
	/**
	  * Creates a new border
	  * @param sizes Border insets
	  * @param color Border color
	  * @param inner The border inside this one
	  * @return A new border with another border inside
	  */
	def apply(sizes: Insets, color: Color, inner: Border): Border = Border(sizes, Some(color), Some(inner))
	
	/**
	  * @param direction Targeted direction
	  * @param width Width of the border on that direction
	  * @param color Border color
	  * @return A new border that only affects the targeted direction
	  */
	def towards(direction: Direction2D, width: Double, color: Color) = Border(Insets.towards(direction, width), color)
	
	/**
	  * @param height Height of this border
	  * @param color Border color
	  * @return A border that only affects the top of a component
	  */
	def top(height: Double, color: Color) = towards(Up, height, color)
	/**
	  * @param height Height of this border
	  * @param color Border color
	  * @return A border that only affects the bottom of a component
	  */
	def bottom(height: Double, color: Color) = towards(Down, height, color)
	/**
	  * @param width Width of this border
	  * @param color Border color
	  * @return A border that only affects the left side of a component
	  */
	def left(width: Double, color: Color) = towards(Direction2D.Left, width, color)
	/**
	  * @param width Width of this border
	  * @param color Border color
	  * @return A border that only affects the right side of a component
	  */
	def right(width: Double, color: Color) = towards(Direction2D.Right, width, color)
	
	/**
	  * Creates a new border where each side is equal
	  * @param side The width of each side
	  * @param color Color of this border
	  * @return A new border
	  */
	@deprecated("Please use .symmetric(Double, Color) instead", "v1.1.1")
	def square(side: Double, color: Color) = symmetric(side, color)
	/**
	  * Creates a symmetric border
	  * @param side Length of each side
	  * @param color Color used in this border
	  * @return A symmetric border
	  */
	def symmetric(side: Double, color: Color) = Border(Insets.symmetric(side), color)
	/**
	  * Creates a symmetric border
	  * @param hBorder Left & right insets
	  * @param vBorder Top & bottom insets
	  * @param color Border color
	  * @return A new border
	  */
	def symmetric(hBorder: Double, vBorder: Double, color: Color) = Border(Insets.symmetric(hBorder, vBorder), color)
	/**
	  * Creates a symmetric border
	  * @param insets Total border insets
	  * @param color Border color
	  * @return A new border
	  */
	def symmetric(insets: Size, color: Color): Border = Border(Insets.symmetric(insets), color)
	
	/**
	  * Creates a horizontal border
	  * @param left Left side insets
	  * @param right Right side insets
	  * @param color Border color
	  * @return A new border
	  */
	def horizontal(left: Double, right: Double, color: Color) = Border(Insets(left, right, 0, 0), color)
	/**
	  * Creates a horizontal border
	  * @param hBorder left & right insets
	  * @param color Border color
	  * @return A new border
	  */
	def horizontal(hBorder: Double, color: Color): Border = horizontal(hBorder, hBorder, color)
	/**
	  * Creates a vertical border
	  * @param top Top insets
	  * @param bottom Bottom insets
	  * @param color Border color
	  * @return A new border
	  */
	def vertical(top: Double, bottom: Double, color: Color) = Border(Insets(0, 0, top, bottom), color)
	/**
	  * Creates a vertical border
	  * @param vBorder Top & bottom insets
	  * @param color Border color
	  * @return A new border
	  */
	def vertical(vBorder: Double, color: Color): Border = vertical(vBorder, vBorder, color)
	
	/**
	  * Creates an empty border
	  * @param insets Border insets
	  * @return An empty border
	  */
	def empty(insets: Insets) = Border(insets, None)
	
	/**
	  * Creates a new border that looks like it was raised
	  * @param w The width of each side
	  * @param baseColor Base color (actual colors will vary based on variance mod)
	  * @param intensity The intensity of color change applied where 0 is no change and 1 is the default change
	  * @return A new border
	  */
	def raised(w: Double, baseColor: Color, intensity: Double = 1.0) = {
		val dark = Border(Insets(0, w, 0, w), baseColor.darkenedBy(intensity))
		val light = Border(dark.insets.opposite, baseColor.lightenedBy(intensity), dark)
		
		light
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
}

/**
  * Borders are placed inside component bounds. They have a size and a color
  * @author Mikko Hilpinen
  * @since 29.4.2019, Reflection v1+
  */
case class Border(insets: Insets, color: Option[Color], inner: Option[Border] = None)
{
	// COMPUTED	-------------------
	
	/**
	  * @return An awt representation of this border
	  */
	def toAwt: javax.swing.border.Border = {
		val myPart = color.map { c => new MatteBorder(insets.toAwt, c.toAwt) } getOrElse new EmptyBorder(insets.toAwt)
		inner.map { i => BorderFactory.createCompoundBorder(myPart, i.toAwt) } getOrElse myPart
	}
	
	/**
	  * @return The total insets of this border
	  */
	def totalInsets: Insets = inner.map { _.totalInsets + insets } getOrElse insets
	
	/**
	  * @return A copy of this border where left is right and top is bottom
	  */
	def opposite: Border = Border(insets.opposite, color, inner.map { _.opposite })
	
	/**
	  * @return A copy of this border where left and right have been swapped
	  */
	def hMirrored: Border = Border(insets.hMirrored, color, inner.map { _.hMirrored })
	/**
	  * @return A copy of this border where top and bottom have been swapped
	  */
	def vMirrored: Border = Border(insets.vMirrored, color, inner.map { _.vMirrored })
	
	
	// OPERATORS	--------------
	
	/**
	  * Multiplies the width of this border
	  * @param multiplier A multiplier
	  * @return A multiplied border
	  */
	def *(multiplier: Double): Border = Border(insets * multiplier, color, inner.map { _ * multiplier })
	
	/**
	  * Divides the width of this border
	  * @param div A divider
	  * @return A divided border
	  */
	def /(div: Double) = this * (1/div)
	
	/**
	  * Combines this border with another
	  * @param another Another border
	  * @return A copy of this border with another border inside
	  */
	def +(another: Border): Border = inner.map { i => Border(insets, color, Some(i + another)) } getOrElse
		Border(insets, color, Some(another))
	
	/**
	  * Removes a border from this border
	  * @param another Another border
	  * @return A copy of this border without the provided border inside
	  */
	def -(another: Border): Border = inner.map {i => if (i == another) Border(insets, color) else
		Border(insets, color, Some(i - another)) } getOrElse this
	
	
	// OTHER	------------------
	
	/**
	  * Maps the color of this border
	  * @param f A mapping function
	  * @return A copy of this border with mapped color
	  */
	def mapColor(f: Color => Color): Border = Border(insets, color.map(f), inner.map { _.mapColor(f) })
	
	/**
	  * Creates a new border with this border inside it
	  * @param another Another border
	  * @return A copy of the other border with this one inside it
	  */
	def inside(another: Border) = another + this
}
