package utopia.reflection.util

import javax.swing.RepaintManager
import utopia.flow.async.Volatile
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Point, Vector2D}
import utopia.genesis.util.Drawer
import utopia.reflection.component.reach.template.ReachComponentLike

import scala.collection.immutable.VectorBuilder

object RealTimeReachPaintManager
{
	/**
	  * Creates a new repaint manager
	  * @param component Component to paint
	  * @param cursor Function for cursor location + image
	  * @param cursorBounds Function for assumed cursor bounds
	  * @return A new paint manager
	  */
	def apply(component: ReachComponentLike)(cursor: => Option[(Point, Image)])(cursorBounds: => Option[Bounds]) =
		new RealTimeReachPaintManager(component)(cursor)(cursorBounds)
}

/**
  * This paint manager implementation attempts to serve component paint requests as soon as possible, without using
  * buffering
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2
  */
// TODO: Add buffering
class RealTimeReachPaintManager(component: ReachComponentLike)(cursor: => Option[(Point, Image)])
							   (cursorBounds: => Option[Bounds])
	extends PaintManager
{
	// ATTRIBUTES	---------------------------------
	
	private lazy val jComponent = component.parentCanvas.component
	
	// Area being drawn currently -> queued areas
	private val queuePointer = Volatile[(Option[Bounds], Map[Priority, Vector[Bounds]])](None -> Map())
	
	
	// IMPLEMENTED	---------------------------------
	
	override def paintWith(drawer: Drawer) =
	{
		// Paints the component, then the cursor
		component.paintWith(drawer)
		cursor.foreach { case (point, image) => image.drawWith(drawer, point) }
	}
	
	override def repaint(region: Option[Bounds], priority: Priority) = region match
	{
		case Some(region) =>
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
	
	override def shift(originalArea: Bounds, transition: Vector2D) = paint { _.copyArea(originalArea, transition) }
	
	
	// OTHER	-------------------------------------
	
	// Pass None if whole component should be painted
	private def paintQueue(first: Option[Bounds]) =
	{
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
	}
	
	private def paintArea(drawer: Drawer, region: Bounds) =
	{
		drawer.clippedTo(region).disposeAfter { drawer =>
			// First paints the component, then possibly mouse
			component.paintWith(drawer, Some(region))
			if (cursorBounds.exists { _.overlapsWith(region) })
				cursor.foreach { case (p, image) => image.drawWith(drawer, p) }
		}
	}
	
	private def paint(f: Drawer => Unit) =
	{
		// Painting is performed in the AWT event thread
		AwtEventThread.async {
			// Suppresses double buffering for the duration of the paint operation
			val repaintManager = RepaintManager.currentManager(jComponent)
			val wasDoubleBuffered = repaintManager.isDoubleBufferingEnabled
			
			repaintManager.setDoubleBufferingEnabled(false)
			// Paints using the component's own graphics instance (which may not be available)
			Option(jComponent.getGraphics).foreach { g =>
				Drawer.use(g)(f)
			}
			repaintManager.setDoubleBufferingEnabled(wasDoubleBuffered)
		}
	}
}
