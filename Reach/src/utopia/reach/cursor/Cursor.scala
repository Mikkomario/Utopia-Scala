package utopia.reach.cursor

import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.image.SingleColorIcon
import utopia.genesis.graphics.StrokeSettings
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.transform.Adjustment

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
	  * @param drawEdges Whether contrasting edges should be drawn for this cursor
	  * @return A cursor that uses the specified icon with additional colouring
	  */
	def apply(icon: SingleColorIcon, drawEdges: Boolean = false): Cursor =
		if (drawEdges) CursorWithEdges(icon) else SingleColorCursor(icon)
	
	
	// NESTED	----------------------------------
	
	private case class StaticImageCursor(image: Image) extends Cursor
	{
		override def defaultBounds = image.bounds
		
		override def over(color: Color) = image
		override def proposing(color: Color) = image
		
		override def apply(shade: => ColorShade) = image
	}
	
	private case class SingleColorCursor(icon: SingleColorIcon) extends Cursor
	{
		// IMPLEMENTED	--------------------------
		
		override def defaultBounds = icon.original.bounds
		
		override def over(color: Color) = icon.against(color)
		override def proposing(color: Color) = icon(color)
		
		override def apply(shade: => ColorShade) = icon(shade)
	}
	
	private case class CursorWithEdges(icon: SingleColorIcon) extends Cursor
	{
		// ATTRIBUTES   -------------------------
		
		private val edgesIcon = icon
			.map { _.paintEdges(Adjustment(0.7), onlyEdges = true)(StrokeSettings.default) }
		private val lightCursor = icon.white.withOverlay(edgesIcon.black)
		private val darkCursor = icon.black.withOverlay(edgesIcon.white)
		
		
		// IMPLEMENTED  -------------------------
		
		override def defaultBounds: Bounds = icon.original.bounds
		
		override def over(color: Color): Image = over(color.shade)
		override def proposing(color: Color): Image = {
			if (color == Color.white)
				lightCursor
			else if (color == Color.black)
				darkCursor
			else
				icon(color).withOverlay(edgesIcon(color.highlightedBy(2.0)))
		}
		override def apply(shade: => ColorShade): Image = shade match {
			case Light => lightCursor
			case Dark => darkCursor
		}
	}
}

/**
  * Cursors specify the image that should be drawn at the mouse cursor location
  * @author Mikko Hilpinen
  * @since 11.11.2020, v0.1
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
	def apply(shade: => ColorShade): Image
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return A lighter version of this cursor suitable against dark backgrounds
	  */
	def light = apply(Light)
	/**
	  * @return A darker version of this cursor suitable against light backgrounds
	  */
	def dark = apply(Dark)
	
	/**
	  * @param context Current component context
	  * @return Cursor suitable for that context
	  */
	def contextual(implicit context: StaticColorContext) = over(context.background)
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param shade Shade of the underlying surface / component
	  * @return A cursor image that looks best against the specified shade
	  */
	def over(shade: => ColorShade) = apply(shade.opposite)
}
