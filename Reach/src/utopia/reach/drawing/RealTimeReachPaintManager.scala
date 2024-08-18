package utopia.reach.drawing

import utopia.firmament.awt.AwtEventThread
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.Positive
import utopia.flow.view.mutable.async.{Volatile, VolatileOption}
import utopia.genesis.graphics.{Drawer, PaintManager, Priority}
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.template.ReachComponentLike

import java.awt.{Graphics2D, Toolkit}
import javax.swing.RepaintManager
import scala.annotation.unused
import scala.collection.immutable.VectorBuilder
import scala.util.Try

object RealTimeReachPaintManager
{
	/**
	  * Creates a new repaint manager
	  * @param component Component to paint
	  * @param background Background color for filling the painted area (optional, call-by-name)
	  * @param maxQueueSize The maximum amount of paint updates that can be queued before the whole component
	  *                     is repainted instead (default = 30)
	  * @param disableDoubleBuffering Whether double buffering should be disabled during draw operations.
	  *                               This is to make the drawing process faster (default = true)
	  * @param syncAfterDraw Whether display syncing should be activated after a single draw event has completed.
	  *                      This is to make the drawing results more responsive (default = true)
	  * @return A new paint manager
	  */
	def apply(component: ReachComponentLike, background: => Option[Color] = None,
	          maxQueueSize: Int = 30, disableDoubleBuffering: Boolean = true, syncAfterDraw: Boolean = true) =
		new RealTimeReachPaintManager(component, background, maxQueueSize, disableDoubleBuffering, syncAfterDraw)
}

/**
  * This paint manager implementation attempts to serve component paint requests as soon as possible, without using
  * buffering
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
// TODO: Add a position modifier (call by name) that affects all draw operations
//  (used for moving window contents while still keeping component position as (0,0))
class RealTimeReachPaintManager(component: ReachComponentLike, background: => Option[Color] = None, maxQueueSize: Int = 30,
                                disableDoubleBuffering: Boolean = true, syncAfterDraw: Boolean = true)
	extends PaintManager
{
	// ATTRIBUTES	---------------------------------
	
	private lazy val canvas = component.parentCanvas
	private lazy val jComponent = canvas.component
	
	// Area being drawn currently -> queued areas
	private val queuePointer = Volatile[(Option[Bounds], Map[Priority, Seq[Bounds]])](None -> Map())
	private val bufferSizePointer = Volatile(Size.zero)
	// TODO: Consider using a MutableImage as a buffer?
	private val bufferPointer = VolatileOption[Image]()
	// None while overfilled, a vector of update images otherwise
	private val queuedUpdatesPointer = VolatileOption[Seq[(Image, Point)]]()
	
	
	// COMPUTED ----------------------------------
	
	private def componentImage = {
		val base = component.toImage
		// Applies background, if necessary
		background match {
			case Some(bg) => base.withBackground(bg)
			case None => base
		}
	}
	
	
	// IMPLEMENTED	---------------------------------
	
	override def paintWith(drawer: Drawer) = {
		// Checks whether component size changed. Invalidates buffer if so.
		checkForSizeChanges()
		flatten().drawWith(drawer, component.position)
	}
	
	override def repaint(region: Option[Bounds], priority: Priority) = region.map { _.ceil } match {
		case Some(region) =>
			// Extends the queue. May start the drawing process as well
			val firstDrawArea = queuePointer.mutate { case (processing, queue) =>
				// Case: No draw process currently active => starts drawing
				if (processing.isEmpty)
					Some(region) -> (Some(region) -> queue)
				// Case: Draw process currently active => queues the region
				else {
					// Checks whether the region could be merged into one of the existing regions
					val existingRegions = queue.getOrElse(priority, Empty)
					val newRegions = {
						if (existingRegions.isEmpty)
							Single(region)
						else {
							val newRegionsBuilder = new VectorBuilder[Bounds]()
							var mergeSucceeded = false
							existingRegions.foreach { b =>
								if (mergeSucceeded)
									newRegionsBuilder += b
								else {
									// Merge is considered successful is less than 10% of extra space is added
									val merged = Bounds.around(Pair(region, b))
									if (merged.area <= (region.area + b.area) * 1.1) {
										mergeSucceeded = true
										newRegionsBuilder += merged
									}
									else
										newRegionsBuilder += b
								}
							}
							if (!mergeSucceeded)
								newRegionsBuilder += region
							newRegionsBuilder.result()
						}
					}
					None -> (processing -> (queue + (priority -> newRegions)))
				}
			}
			if (firstDrawArea.nonEmpty)
				paintQueue(firstDrawArea)
		case None =>
			// Paints the whole component, clearing the queue
			val componentBounds = Bounds(Point.origin, component.size)
			val shouldPaintNow = queuePointer.mutate { case (processing, _) =>
				if (processing.nonEmpty)
					false -> (processing -> Map(priority -> Single(componentBounds)))
				else
					true -> (Some(componentBounds) -> Map())
			}
			if (shouldPaintNow)
				paintQueue(None)
	}
	
	override def shift(originalArea: Bounds, transition: Vector2D) = {
		if (transition.nonZero) {
			// Copies the area
			paint { _.copyArea(originalArea, transition) }
			// Invalidates buffer
			bufferPointer.clear()
			// Repaints the old area
			Axis2D.values.find { transition(_) == 0.0 } match {
				// If the translation only affected one axis, uses minimum repaint area
				case Some(noMovementAxis) =>
					val movementAxis = noMovementAxis.perpendicular
					val transitionAmount = transition(movementAxis)
					val overlapAmount = originalArea.size(movementAxis) - transitionAmount.abs
					if (overlapAmount > 0.0) {
						// Case: Moving right or down => area at the end of the original bounds is repainted
						// Case: Moving left or up => area at the start of the original bounds is repainted
						repaintRegion(originalArea.slice(
							movementAxis.toDirection(Sign.of(transitionAmount).binaryOr(Positive)), overlapAmount))
					}
					else
						repaintRegion(originalArea)
				// Otherwise paints the original area completely
				case None => repaintRegion(originalArea)
			}
		}
	}
	
	
	// OTHER	-------------------------------------
	
	// TODO: Possibly remove - This function was previously part of the PaintManager interface
	def paint(region: Option[Bounds], @unused priority: Priority): Unit = {
		// May have to repaint if component size had changed since the last paint
		checkForSizeChanges()
		// Makes sure the image buffer is up to date
		val newImage = flatten(region)
		// Paints the image buffer, at least partially
		paint { drawer =>
			val modifiedDrawer = region match {
				case Some(region) => drawer.clippedToBounds(region)
				case None => drawer
			}
			newImage.drawWith(modifiedDrawer, component.position)
		}
	}
	
	// Updates the buffer and processes data from the image
	def averageShadeOf(area: Bounds) = ColorShade.forLuminosity(flatten().averageRelativeLuminanceOf(area))
	
	/**
	  * @return Resets the buffer, so that the next draw operation will completely redraw the component contents
	  */
	def resetBuffer() = bufferPointer.clear()
	
	// Checks whether component size has changed since the last draw. Invalidates the drawn buffer if so.
	private def checkForSizeChanges() =
	{
		val currentSize = component.size
		val sizeWasChanged = bufferSizePointer.mutate { old => (old != currentSize) -> currentSize }
		if (sizeWasChanged)
			bufferPointer.clear()
	}
	
	// Makes sure the buffer is updated (prepares the buffer applies any queued updates)
	// Accepts a region to flatten. None if whole image should be flattened.
	// If Some(X), updates outside area X are not applied yet.
	private def flatten(region: Option[Bounds] = None) = {
		// Prepares the buffer
		val (shouldAddUpdates, baseImage) = bufferPointer.mutate {
			// Case: There already exists a buffer => Uses it, adding updates on top
			case Some(existing) => (true, existing) -> Some(existing)
			// Case: No buffer exists => Repaints the whole component
			case None =>
				val newImage = componentImage
				(false, newImage) -> Some(newImage)
		}
		
		// Applies or discards updates
		if (shouldAddUpdates) {
			// If the update buffer was overfilled (None), recreates the buffer completely
			queuedUpdatesPointer.mutate {
				// Case: Update buffer was not overfilled
				case Some(updates) =>
					// Checks for updates to apply, based on the targeted region
					val (updatesToDelay, updatesToApply) = region match {
						case Some(region) =>
							updates
								.divideBy { case (image, position) => Bounds(position, image.size).overlapsWith(region) }
								.toTuple
						case None => Empty -> updates
					}
					// Calculates the new buffer state
					val newImage = {
						// Case: There were updates queued => Updates the buffer also
						if (updatesToApply.nonEmpty) {
							val updatedBuffer = updatesToApply.foldLeft(baseImage) { (image, update) =>
								image.withOverlay(update._1, update._2)
							}
							bufferPointer.setOne(updatedBuffer)
							updatedBuffer
						}
						// Case: No updates were queued
						else
							baseImage
					}
					newImage -> Some(updatesToDelay)
				// Case: There were too many updates
				case None =>
					val newImage = componentImage
					bufferPointer.setOne(newImage)
					newImage -> Some(Empty)
			}
		}
		else {
			queuedUpdatesPointer.setOne(Empty)
			baseImage
		}
	}
	
	// Pass None if whole component should be painted
	private def paintQueue(first: Option[Bounds]) = {
		paint { drawer =>
			var nextArea = first
			do {
				// Paints the next area, continues as long as areas can be pulled
				nextArea match {
					case Some(region) => paintArea(drawer, region)
					case None => paintWith(drawer)
				}
				nextArea = queuePointer.mutate { case (_, queue) =>
					// Picks the next highest priority area (preferring smaller areas)
					Priority.descending.find(queue.contains) match {
						case Some(targetPriority) =>
							val options = queue(targetPriority)
							if (options.size > 1) {
								val next = options.minBy { _.area }
								Some(next) -> (Some(next), queue + (targetPriority -> options.filterNot { _ == next }))
							}
							else
								options.headOption -> (options.headOption, queue - targetPriority)
						// Case: No more queues left
						case None => None -> (None, queue)
					}
				}
			}
			while (nextArea.nonEmpty)
		}
	}
	
	private def paintArea(drawer: Drawer, region: Bounds) = {
		// First draws the component region to a separate image
		val buffered = {
			val base = component.regionToImage(region)
			background match {
				case Some(bg) => base.withBackground(bg)
				case None => base
			}
		}
		// Draws the buffered area using the drawer (may also draw the cursor)
		drawer.clippedToBounds(region).use { d => buffered.drawWith(d, component.position + region.position) }
		// Queues the buffer to be drawn when component will be fully painted next time
		queuedUpdatesPointer.update {
			case Some(queue) =>
				// If the queue reaches maximum size, invalidates it
				if (queue.size < maxQueueSize)
					Some(queue.filterNot { case (image, position) =>
						region.contains(Bounds(position, image.size)) } :+ (buffered -> region.position))
				else
					None
			case None => None
		}
	}
	
	private def paint(f: Drawer => Unit) = {
		// Painting is performed in the AWT event thread
		AwtEventThread.async {
			if (disableDoubleBuffering) {
				// Suppresses double buffering for the duration of the paint operation (optional)
				val repaintManager = RepaintManager.currentManager(jComponent)
				val wasDoubleBuffered = repaintManager.isDoubleBufferingEnabled
				
				repaintManager.setDoubleBufferingEnabled(false)
				draw(f)
				repaintManager.setDoubleBufferingEnabled(wasDoubleBuffered)
			}
			else
				draw(f)
		}
	}
	
	// Doesn't perform any preparation, as in paint
	private def draw(f: Drawer => Unit) = {
		// Paints using the component's own graphics instance (which may not be available)
		Option(jComponent.getGraphics).foreach { g => Drawer(g.asInstanceOf[Graphics2D]).use(f) }
		if (syncAfterDraw)
			Try { Toolkit.getDefaultToolkit.sync() }
	}
}
