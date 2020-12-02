package utopia.reflection.cursor

import utopia.flow.caching.multi.TryCache
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.reflection.color.ColorShadeVariant
import utopia.reflection.component.reach.template.CursorDefining

import java.awt.Toolkit
import java.awt.image.BufferedImage
import scala.util.Try

/**
  * Used for determining, which cursor image should be drawn
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
class ReachCursorManager(val cursors: CursorSet)
{
	// ATTRIBUTES	-----------------------------
	
	private val cursorIndexGenerator = Iterator.iterate(1) { _ + 1 }
	private lazy val blankCursor = Try { Toolkit.getDefaultToolkit.createCustomCursor(
		new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new java.awt.Point(0, 0),
		"blank cursor") }
	
	private var cursorComponents = Vector[CursorDefining]()
	
	// FIXME: Cursors appear twice as large in the actual program (cause: windows always uses 32x32 cursors. Will need to scale accordingly)
	private val cursorCache = TryCache.releasing[Image, java.awt.Cursor](1.minutes, 3.minutes) { image =>
		// Applies alpha changes directly to the image, if necessary
		val appliedImage =
		{
			if (image.alpha >= 1.0)
				image
			else
				image.withAlpha(1.0).mapPixels { _.timesAlpha(image.alpha) }
		}
		appliedImage.toAwt match
		{
			case Some(awtImage) =>
				// Converts the source image to a new cursor
				Try {
					Toolkit.getDefaultToolkit.createCustomCursor(awtImage,
						appliedImage.sourceResolutionOrigin.toAwtPoint,
						s"Reach-cursor-${cursorIndexGenerator.next()}")
				}
			case None => blankCursor
		}
	}
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param image A cursor image
	  * @return A cursor to use with the image. Failure if cursor creation failed.
	  */
	def cursorForImage(image: Image) = cursorCache(image)
	
	/**
	  * @param position A position in the cursor managed context
	  * @param shadeOf A function for calculating the overall shade of the targeted area
	  * @return Cursor to use over that position. Failure if the cursor couldn't be created.
	  */
	def cursorAt(position: Point)(shadeOf: Bounds => ColorShadeVariant) =
		cursorCache(cursorImageAt(position)(shadeOf))
	
	/**
	  * @param position A position in the cursor managed context
	  * @param shadeOf A function for calculating the overall shade of the targeted area
	  * @return Cursor image to use over that position
	  */
	def cursorImageAt(position: Point)(shadeOf: Bounds => ColorShadeVariant) =
	{
		// Checks whether any of the registered components is controlling the specified position
		cursorComponents.findMap { c =>
			val bounds = c.cursorBounds
			if (bounds.contains(position))
				Some(c -> bounds)
			else
				None
		} match
		{
			// Case: Component manages area => lets the component decide cursor styling
			case Some((component, bounds)) =>
				component.cursorToImage(cursors(component.cursorType), position - bounds.position)
			// Case: Cursor is outside all registered component zones => uses default cursor with modified shade
			case None =>
				val cursor = cursors.default
				cursor(shadeOf(cursor.defaultBounds.translated(position)).opposite)
		}
	}
	
	/**
	  * Registers a component to affect the selected cursor
	  * @param component A component to affect cursor selection from now on
	  */
	def registerComponent(component: CursorDefining) = cursorComponents :+= component
	
	/**
	  * Removes a component from affecting the selected cursor
	  * @param component A component to no longer consider when selecting cursor
	  */
	def unregisterComponent(component: Any) = cursorComponents = cursorComponents.filterNot { _ == component }
	
	/**
	  * Registers a component to affect the selected cursor
	  * @param component A component to affect cursor selection from now on
	  */
	def +=(component: CursorDefining) = registerComponent(component)
	
	/**
	  * Removes a component from affecting the selected cursor
	  * @param component A component to no longer consider when selecting cursor
	  */
	def -=(component: Any) = unregisterComponent(component)
}
