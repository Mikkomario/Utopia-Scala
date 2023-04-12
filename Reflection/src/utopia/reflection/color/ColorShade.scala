package utopia.reflection.color

/**
  * An enumeration for standard color shades used in various UI components
  * @author Mikko Hilpinen
  * @since 18.8.2020, v1.2
  */
@deprecated("Moved to Paradigm as ColorLevel", "v2.0")
sealed trait ColorShade
{
	/**
	  * @param shade A color shade variant
	  * @return Next shade towards that variant. May return this shade if this shade is as far as it gets
	  *         towards that direction.
	  */
	def nextTowards(shade: ColorShadeVariant): ColorShade
}

@deprecated("Moved to Paradigm as ColorShade", "v2.0")
sealed trait ColorShadeVariant extends ColorShade
{
	/**
	  * @return A shade variant opposite to this one
	  */
	def opposite: ColorShadeVariant
}

@deprecated("Moved to Paradigm as ColorLevel", "v2.0")
object ColorShade
{
	/**
	  * Standard middle color shade / the default color shade
	  */
	@deprecated("Moved to Paradigm", "v2.0")
	case object Standard extends ColorShade
	{
		override def nextTowards(shade: ColorShadeVariant) = shade
	}
	
	/**
	  * Lighter color shade / variant
	  */
	@deprecated("Moved to Paradigm", "v2.0")
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
	@deprecated("Moved to Paradigm", "v2.0")
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

@deprecated("Moved to Paradigm as ColorShade", "v2.0")
object ColorShadeVariant
{
	import ColorShade._
	
	/**
	  * @param luminosity A luminosity level
	  * @return A color shade variant matching that luminosity level
	  */
	def forLuminosity(luminosity: Double) = if (luminosity > 0.5) Light else Dark
}
