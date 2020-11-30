package utopia.reflection.util

import java.awt.Toolkit

import javax.swing.RepaintManager
import utopia.flow.async.{Volatile, VolatileOption}
import utopia.flow.util.TimeLogger
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Point, Size, Vector2D}
import utopia.genesis.util.Drawer
import utopia.reflection.component.reach.template.ReachComponentLike

import scala.collection.immutable.VectorBuilder
import scala.util.Try

object RealTimeReachPaintManager
{
	/**
	  * Creates a new repaint manager
	  * @param component Component to paint
	  * @param maxQueueSize The maximum amount of paint updates that can be queued before the whole component
	  *                     is repainted instead (default = 30)
	  * @param cursor Function for cursor location + image
	  * @param cursorBounds Function for assumed cursor bounds
	  * @return A new paint manager
	  */
	def apply(component: ReachComponentLike, maxQueueSize: Int = 30)(cursor: => Option[(Point, Image)])
			 (cursorBounds: => Option[Bounds]) =
		new RealTimeReachPaintManager(component)(cursor)(cursorBounds)
}

/**
  * This paint manager implementation attempts to serve component paint requests as soon as possible, without using
  * buffering
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2
  */
class RealTimeReachPaintManager(component: ReachComponentLike, maxQueueSize: Int = 30)
							   (cursor: => Option[(Point, Image)])(cursorBounds: => Option[Bounds])
	extends PaintManager
{
	// ATTRIBUTES	---------------------------------
	
	private lazy val canvas = component.parentCanvas
	private lazy val jComponent = canvas.component
	
	// Area being drawn currently -> queued areas
	private val queuePointer = Volatile[(Option[Bounds], Map[Priority, Vector[Bounds]])](None -> Map())
	private val bufferSizePointer = Volatile(Size.zero)
	private val bufferPointer = VolatileOption[Image]()
	private val queuedUpdatesPointer = VolatileOption[Vector[(Image, Point)]]()
	
	private val tracker = new TimeLogger()
	
	
	// IMPLEMENTED	---------------------------------
	
	override def paintWith(drawer: Drawer) =
	{
		tracker.checkPoint("Starting full repaint")
		// Checks whether component size changed. Invalidates buffer if so.
		val currentSize = canvas.size
		val sizeWasChanged = bufferSizePointer.pop { old => (old != currentSize) -> currentSize }
		if (sizeWasChanged)
		{
			/*
			drawer.onlyFill(Color.magenta).draw(Bounds(Point.origin, currentSize))
			bufferPointer.pop().foreach { buffer => buffer.drawWith(drawer) }
			println("Painted placeholder")
			 */
			bufferPointer.clear()
		}
		
		// Updates the buffer, then paints it
		val (shouldAddUpdates, baseImage) = bufferPointer.pop {
			case Some(existing) => (true, existing) -> Some(existing)
			case None =>
				val newImage = component.toImage
				(false, newImage) -> Some(newImage)
		}
		val imageToPaint =
		{
			if (shouldAddUpdates)
			{
				// If the update buffer was overfilled, recreates the buffer completely
				queuedUpdatesPointer.getAndSet(Some(Vector())) match
				{
					case Some(updates) =>
						println(s"Painting ${updates.size} updates")
						updates.foldLeft(baseImage) { (image, update) =>
							image.withOverlay(update._1, update._2)
						}
					case None =>
						val newImage = component.toImage
						bufferPointer.setOne(newImage)
						newImage
				}
			}
			else
			{
				println("Painting all again")
				queuedUpdatesPointer.setOne(Vector())
				baseImage
			}
		}
		imageToPaint.drawWith(drawer)
		
		// Paints the cursor afterwards
		tracker.checkPoint("Painting cursor")
		cursor.foreach { case (point, image) => image.drawWith(drawer, point) }
		tracker.checkPoint("Finished cursor painting")
	}
	
	override def repaint(region: Option[Bounds], priority: Priority) = region.map { _.ceil } match
	{
		case Some(region) =>
			tracker.checkPoint("Starting region painting")
			// Extends the queue. May start the drawing process as well
			val firstDrawArea = queuePointer.pop { case (processing, queue) =>
				// Case: No draw process currently active => starts drawing
				if (processing.isEmpty)
					Some(region) -> (Some(region) -> queue)
				// Case: Draw process currently active => queues the region
				else
				{
					// Checks whether the region could be merged into one of the existing regions
					val existingRegions = queue.getOrElse(priority, Vector())
					val newRegions =
					{
						if (existingRegions.isEmpty)
							Vector(region)
						else
						{
							val newRegionsBuilder = new VectorBuilder[Bounds]()
							var mergeSucceeded = false
							existingRegions.foreach { b =>
								if (mergeSucceeded)
									newRegionsBuilder += b
								else
								{
									// Merge is considered successful is less than 10% of extra space is added
									val merged = Bounds.around(Vector(region, b))
									if (merged.area <= (region.area + b.area) * 1.1)
									{
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
			val shouldPaintNow = queuePointer.pop { case (processing, _) =>
				if (processing.nonEmpty)
					false -> (processing -> Map(priority -> Vector(componentBounds)))
				else
					true -> (Some(componentBounds) -> Map())
			}
			if (shouldPaintNow)
				paintQueue(None)
	}
	
	override def shift(originalArea: Bounds, transition: Vector2D) =
	{
		paint { _.copyArea(originalArea, transition) }
		bufferPointer.clear()
	}
	
	
	// OTHER	-------------------------------------
	
	// Pass None if whole component should be painted
	private def paintQueue(first: Option[Bounds]) =
	{
		tracker.checkPoint("Painting first item in queue")
		paint { drawer =>
			var nextArea = first
			do
			{
				// Paints the next area, continues as long as areas can be pulled
				nextArea match
				{
					case Some(region) => paintArea(drawer, region)
					case None => paintWith(drawer)
				}
				tracker.checkPoint("Painting queue updates")
				nextArea = queuePointer.pop { case (_, queue) =>
					// Picks the next highest priority area (preferring smaller areas)
					Priority.descending.find(queue.contains) match
					{
						case Some(targetPriority) =>
							val options = queue(targetPriority)
							if (options.size > 1)
							{
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
		tracker.checkPoint("Queue painting finished")
	}
	
	private def paintArea(drawer: Drawer, region: Bounds) =
	{
		// First draws the component region to a separate image
		val buffered = component.regionToImage(region)
		// May add the cursor to the buffered image
		val drawn =
		{
			if (cursorBounds.exists { _.overlapsWith(region) })
			{
				cursor match
				{
					case Some((p, cursorImage)) => buffered.withOverlay(cursorImage, p - region.position)
					case None => buffered
				}
			}
			else
				buffered
		}
		// Draws the buffered area using the drawer (may also draw the cursor)
		drawer.clippedTo(region).disposeAfter { d => drawn.drawWith(d, region.position) }
		// Queues the buffer to be drawn when component will be fully painted next time
		queuedUpdatesPointer.update
		{
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
	
	private def paint(f: Drawer => Unit) =
	{
		tracker.checkPoint("Accessing awt thread")
		// Painting is performed in the AWT event thread
		AwtEventThread.async {
			tracker.checkPoint("Starting paint")
			// Suppresses double buffering for the duration of the paint operation
			val repaintManager = RepaintManager.currentManager(jComponent)
			val wasDoubleBuffered = repaintManager.isDoubleBufferingEnabled
			
			repaintManager.setDoubleBufferingEnabled(false)
			// Paints using the component's own graphics instance (which may not be available)
			Option(jComponent.getGraphics).foreach { g =>
				Drawer.use(g)(f)
			}
			Try { Toolkit.getDefaultToolkit.sync() }
			
			repaintManager.setDoubleBufferingEnabled(wasDoubleBuffered)
			tracker.checkPoint("Finished paint")
		}
	}
}
