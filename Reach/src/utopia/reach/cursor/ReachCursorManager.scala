package utopia.reach.cursor

import utopia.flow.caching.multi.TryCache
import utopia.flow.util.CollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.reflection.color.ColorShadeVariant
import utopia.reach.component.template.CursorDefining

import java.awt.Toolkit
import java.awt.image.BufferedImage
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
/*
object ReachCursorManager
{
	// The forced cursor size in the operating system
	// private val osCursorSize = Toolkit.getDefaultToolkit.getBestCursorSize(1, 1)// Size(32, 32)
}*/

/**
  * Used for determining, which cursor image should be drawn
  * @author Mikko Hilpinen
  * @since 11.11.2020, v0.1
  */
class ReachCursorManager(val cursors: CursorSet)(implicit exc: ExecutionContext)
{
	// ATTRIBUTES	-----------------------------
	
	private val cursorIndexGenerator = Iterator.iterate(1) { _ + 1 }
	private lazy val blankCursor = Try { Toolkit.getDefaultToolkit.createCustomCursor(
		new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new java.awt.Point(0, 0),
		"blank cursor") }
	
	private var cursorComponents = Vector[CursorDefining]()
	
	private val cursorCache = TryCache.releasing[Image, java.awt.Cursor](1.minutes, 5.minutes) { image =>
		// Applies the image with proper os-supported measurements and applied alpha value. Will not crop the image.
		val osCursorSize = Try { Size.of(Toolkit.getDefaultToolkit.getBestCursorSize(
			image.width.round.toInt, image.height.round.toInt)) }
		val correctedImage = osCursorSize match
		{
			case Success(targetSize) => image.fittingWithin(targetSize).paintedToCanvas(targetSize)
			case Failure(_) =>
				if (image.alpha >= 1)
					image
				else
					image.mapPixels { _.timesAlpha(image.alpha) }
		}
		correctedImage.toAwt match
		{
			case Some(awtImage) =>
				// Converts the source image to a new cursor
				Try {
					Toolkit.getDefaultToolkit.createCustomCursor(awtImage,
						correctedImage.sourceResolutionOrigin.toAwtPoint,
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
