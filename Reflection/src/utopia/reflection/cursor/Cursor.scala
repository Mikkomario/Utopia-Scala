package utopia.reflection.cursor

import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Bounds
import utopia.reflection.color.ColorShade.{Dark, Light}
import utopia.reflection.color.{ColorShadeVariant, TextColorStandard}
import utopia.reflection.color.ComponentColor.autoGenerateFromConvertible
import utopia.reflection.image.SingleColorIcon

object Cursor
{
	// OTHER	----------------------------------
	
	/**
	  * @param image A static cursor image
	  * @return A cursor that will always use the specified image
	  */
	def apply(image: Image): Cursor = StaticImageCursor(image)
	
	/**
	  * @param icon A cursor icon (should use only a single color)
	  * @return A cursor that uses the specified icon with additional colouring
	  */
	def apply(icon: SingleColorIcon): Cursor = SingleColorCursor(icon)
	
	
	// NESTED	----------------------------------
	
	private case class StaticImageCursor(image: Image) extends Cursor
	{
		override def defaultBounds = image.bounds
		
		override def over(color: Color) = image
		
		override def proposing(color: Color) = image
		
		override def apply(shade: => ColorShadeVariant) = image
	}
	
	private case class SingleColorCursor(icon: SingleColorIcon) extends Cursor
	{
		// IMPLEMENTED	--------------------------
		
		override def defaultBounds = icon.original.bounds
		
		override def over(color: Color) = color.textColorStandard match
		{
			case TextColorStandard.Light => icon.white
			case TextColorStandard.Dark => icon.black
		}
		
		override def proposing(color: Color) = icon.asImageWithColor(color)
		
		override def apply(shade: => ColorShadeVariant) = icon.withShade(shade)
	}
}

/**
  * Cursors specify the image that should be drawn at the mouse cursor location
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
trait Cursor
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return A set of bounds that characterize this cursor, even though they may not be exact for all available
	  *         images. The (0,0) coordinate of the bounds should be located at the active cursor position.
	  */
	def defaultBounds: Bounds
	
	/**
	  * @param color Color of the element / pixel below the cursor
	  * @return Cursor image to display over that color
	  */
	def over(color: Color): Image
	
	/**
	  * @param color Color proposed as the basis for the resulting cursor color / image
	  * @return Cursor image to display when that color is recommended
	  */
	def proposing(color: Color): Image
	
	/**
	  * @param shade Targeted color shade for the cursor (call by name)
	  * @return An image that best applies to the specified shade
	  */
	def apply(shade: => ColorShadeVariant): Image
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return A lighter version of this cursor suitable against dark backgrounds
	  */
	def light = apply(Light)
	
	/**
	  * @return A darker version of this cursor suitable against light backgrounds
	  */
	def dark = apply(Dark)
}
