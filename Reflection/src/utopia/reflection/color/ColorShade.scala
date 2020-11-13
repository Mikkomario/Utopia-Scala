package utopia.reflection.color

/**
  * An enumeration for standard color shades used in various UI components
  * @author Mikko Hilpinen
  * @since 18.8.2020, v1.2
  */
sealed trait ColorShade
{
	/**
	  * @param shade A color shade variant
	  * @return Next shade towards that variant. May return this shade if this shade is as far as it gets
	  *         towards that direction.
	  */
	def nextTowards(shade: ColorShadeVariant): ColorShade
}

sealed trait ColorShadeVariant extends ColorShade
{
	/**
	  * @return A shade variant opposite to this one
	  */
	def opposite: ColorShadeVariant
}

object ColorShade
{
	/**
	  * Standard middle color shade / the default color shade
	  */
	case object Standard extends ColorShade
	{
		override def nextTowards(shade: ColorShadeVariant) = shade
	}
	
	/**
	  * Lighter color shade / variant
	  */
	case object Light extends ColorShadeVariant
	{
		override def opposite = Dark
		
		override def nextTowards(shade: ColorShadeVariant) = shade match
		{
			case Dark => Standard
			case _ => this
		}
	}
	
	/**
	  * Darker color shade / variant
	  */
	case object Dark extends ColorShadeVariant
	{
		override def opposite = Light
		
		override def nextTowards(shade: ColorShadeVariant) = shade match
		{
			case Light => Standard
			case _ => this
		}
	}
}

object ColorShadeVariant
{
	import ColorShade._
	
	/**
	  * @param luminosity A luminosity level
	  * @return A color shade variant matching that luminosity level
	  */
	def forLuminosity(luminosity: Double) = if (luminosity > 0.5) Light else Dark
}
