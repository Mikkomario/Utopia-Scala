package utopia.paradigm.color

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{Reversible, SelfComparable}

/**
  * An enumeration for standard color shades used in various UI components
  * @author Mikko Hilpinen
  * @since 18.8.2020, Reflection v1.2
  */
sealed trait ColorLevel extends SelfComparable[ColorLevel]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @param shade A color shade variant
	  * @return Next shade towards that variant.
	  *         May return this shade if this shade is as far as it gets towards that direction.
	  */
	def nextTowards(shade: ColorShade): ColorLevel
}

sealed trait ColorShade extends ColorLevel with Reversible[ColorShade]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return A shade variant opposite to this one
	  */
	def opposite: ColorShade
	
	/**
	  * @return The default text color to use against this color level
	  */
	def defaultTextColor: Color
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Default text color to use against this color level, when presenting with hint or disabled text
	  */
	def defaultHintTextColor = defaultTextColor.timesAlpha(0.625)
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: ColorShade = this
	
	override def unary_- : ColorShade = opposite
}

object ColorLevel
{
	import ColorShade._
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Access to color shades
	  */
	def shade = ColorShade
	
	
	// VALUES   ------------------------
	
	/**
	  * Standard middle color shade / the default color shade
	  */
	case object Standard extends ColorLevel
	{
		override def nextTowards(shade: ColorShade) = shade
		
		override def self = this
		
		override def compareTo(o: ColorLevel) = o match {
			case Standard => 0
			case Light => -1
			case Dark => 1
		}
	}
}

object ColorShade
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * All two color shade values (light & dark)
	  */
	val values = Pair[ColorShade](Light, Dark)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param luminosity A luminosity level
	  * @return A color shade variant matching that luminosity level
	  */
	def forLuminosity(luminosity: Double): ColorShade = if (luminosity > 0.5) Light else Dark
	
	
	// VALUES   -----------------------------
	
	/**
	  * Lighter color shade / variant
	  */
	case object Light extends ColorShade
	{
		override def defaultTextColor: Color = Color.textBlack
		
		override def opposite = Dark
		
		override def nextTowards(shade: ColorShade) = shade match {
			case Dark => ColorLevel.Standard
			case _ => this
		}
		
		override def compareTo(o: ColorLevel) = o match {
			case Light => 0
			case _ => 1
		}
	}
	
	/**
	  * Darker color shade / variant
	  */
	case object Dark extends ColorShade
	{
		override def defaultTextColor: Color = Color.textWhite
		
		override def opposite = Light
		
		override def nextTowards(shade: ColorShade) = shade match {
			case Light => ColorLevel.Standard
			case _ => this
		}
		
		override def compareTo(o: ColorLevel) = o match {
			case Dark => 0
			case _ => -1
		}
	}
}

