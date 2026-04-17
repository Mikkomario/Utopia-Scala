package utopia.reach.drawing

import utopia.firmament.awt.AwtEventThread
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.Positive
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.graphics.{Drawer, PaintManager, Priority}
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.template.ReachComponent

import java.awt.{Graphics2D, Toolkit}
import javax.swing.RepaintManager
import scala.annotation.unused
import scala.util.Try

object RealTimeReachPaintManager
{
	/**
	  * Creates a new repaint manager
	  * @param component Component to paint
	  * @param background Background color for filling the painted area (optional, call-by-name)
	  * @param maxQueueSize The maximum amount of paint updates that can be queued before the whole component
	  *                     is repainted instead (default = 30)
	  * @param delayPaintingWhile A flag that contains true while painting should be delayed.
	  *                           This may be the case, for example, during component size changes.
	  *                           Default = never delay.
	  * @param disableDoubleBuffering Whether double buffering should be disabled during draw operations.
	  *                               This is to make the drawing process faster (default = true)
	  * @param syncAfterDraw Whether display syncing should be activated after a single draw event has completed.
	  *                      This is to make the drawing results more responsive (default = true)
	  * @return A new paint manager
	  */
	def apply(component: ReachComponent, background: => Option[Color] = None,
	          maxQueueSize: Int = 30, delayPaintingWhile: Flag = AlwaysFalse,
	          disableDoubleBuffering: Boolean = true, syncAfterDraw: Boolean = true) =
		new RealTimeReachPaintManager(component, background, maxQueueSize, delayPaintingWhile,
			disableDoubleBuffering, syncAfterDraw)
}

/**
  * This paint manager implementation attempts to serve component paint requests as soon as possible, without using
  * buffering
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
// TODO: Add a position modifier (call by name) that affects all draw operations
//  (used for moving window contents while still keeping component position as (0,0))
class RealTimeReachPaintManager(component: ReachComponent, background: => Option[Color] = None,
                                maxQueueSize: Int = 30, delayPaintingFlag: Flag = AlwaysFalse,
                                disableDoubleBuffering: Boolean = true, syncAfterDraw: Boolean = true)
	extends PaintManager
{
	// ATTRIBUTES	---------------------------------
	
	private lazy val canvas = component.parentCanvas
	private lazy val jComponent = canvas.component
	
	/**
	 * A mutable pointer that contains 2 values:
	 *      1. The area currently being drawn
	 *      1. Areas to draw next, grouped by priority
	 */
	private val queuePointer = Volatile[(Option[Bounds], Map[Priority, Seq[Bounds]])](None -> Map())
	private val bufferSizePointer = Volatile(Size.zero)
	// TODO: Consider using a MutableImage as a buffer?
	private val bufferPointer = Volatile.optional[Image]()
	// None while overfilled, a vector of update images otherwise
	private val queuedUpdatesPointer = Volatile.optional[Seq[(Image, Point)]]()
	
	
	// COMPUTED ----------------------------------
	
	private def componentImage = {
		val base = component.toImage
		// Applies background, if necessary
		background match {
			case Some(bg) => base.withBackground(bg)
			case None => base
		}
	}
	
	/**
	 * @return An iterator that removes target regions from [[queuePointer]].
	 *         Yields first the high priority regions, preferring smaller regions.
	 *
	 *         Terminates once the queue has been fully cleared.
	 *
	 *         NB: Discards the processing item slot in the queue pointer.
	 *             Assumes that said region has been completed when next() is called.
	 */
	private def unqueue = OptionsIterator.continually {
		queuePointer.mutate { case (_, queue) =>
			Priority.descending.findMap { priority =>
				queue.get(priority).flatMap { options =>
					options.emptyOneOrMany.map {
						case Left(only) => (only, queue - priority)
						case Right(options) =>
							val next = options.minBy { _.area }
							(next, queue + (priority -> options.filterNot { _ == next }))
					}
				}
			} match {
				case Some((next, remaining)) => (Some(next), Some(next) -> remaining)
				case None => (None, None -> queue)
			}
		}
	}
	
	
	// IMPLEMENTED	---------------------------------
	
	// Ensures the buffer is up-to-date and then paints the component
	override def paintWith(drawer: Drawer) = buffer().drawWith(drawer, component.position)
	
	override def repaint(region: Option[Bounds], priority: Priority) = {
		lazy val componentSize = component.size
		region.map { _.ceil }
			// If the targeted region spans the whole component area, prepares a full repaint instead
			.filterNot { region => region.leftX >= 0 && region.topY <= 0 &&
				region.rightX >= componentSize.width && region.bottomY >= componentSize.height
			} match
		{
			// Case: Painting a subregion => Queues and starts drawing, if appropriate
			case Some(region) =>
				// Extends the queue. May start the drawing process as well
				val shouldDrawNow = queue(region, priority, placeForDrawing = true)
				if (shouldDrawNow)
					paintQueue(Some(region))
			
			// Case: Painting the whole component => Clears the queue and performs painting
			case None =>
				val componentBounds = Bounds(Point.origin, componentSize)
				val shouldPaintNow = queuePointer.mutate { case (processing, _) =>
					if (processing.nonEmpty)
						false -> (processing -> Map(priority -> Single(componentBounds)))
					else
						true -> (Some(componentBounds) -> Map())
				}
				if (shouldPaintNow)
					paintQueue(None)
		}
	}
	
	override def invalidate(region: Option[Bounds], priority: Priority): Unit = region match {
		case Some(region) => queue(region, priority)
		case None => resetBuffer()
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
	  * Buffers the currently queued paint events
	  * @return Buffered component image
	  */
	def buffer() = {
		checkForSizeChanges()
		flatten()
	}
	/**
	  * @return Resets the buffer, so that the next draw operation will completely redraw the component contents
	  */
	def resetBuffer() = bufferPointer.clear()
	
	private def queue(region: Bounds, priority: Priority, placeForDrawing: Boolean = false) =
		queuePointer.mutate { case (processing, queue) =>
			// Case: No draw process currently active & possible to start => Places this region on the drawing position
			if (placeForDrawing && processing.isEmpty)
				true -> (Some(region) -> queue)
			// Case: Draw process currently active (or not requested to trigger) => Queues the region
			else {
				// Checks whether the region could be merged into one of the existing regions
				val newRegions = queue.get(priority).filter { _.nonEmpty } match {
					case Some(existingRegions) =>
						val newRegionsBuilder = OptimizedIndexedSeq.newBuilder[Bounds]
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
					
					case None => Single(region)
				}
				false -> (processing, queue + (priority -> newRegions))
			}
		}
	
	// Checks whether component size has changed since the last draw. Invalidates the drawn buffer if so.
	private def checkForSizeChanges() = {
		val currentSize = component.size
		val sizeWasChanged = bufferSizePointer.mutate { old => (old != currentSize) -> currentSize }
		if (sizeWasChanged)
			bufferPointer.clear()
	}
	
	/**
	 * Makes sure the buffer is updated; I.e. prepares the buffer by applying the queued updates.
	 * @param region Region to buffer. If set to Some, ignores updates outside this region.
	 * @return Component image to draw
	 */
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
			// Buffers the queued updates
			// However, if the update buffer becomes full, discards the queued updates
			if (unqueue.exists { bufferArea(_).nonInitialized })
				queuePointer.update { _._1 -> Map() }
			
			// If the update buffer was overfilled (i.e. contains None), recreates the buffer completely
			queuedUpdatesPointer.mutate {
				// Case: Update buffer was not overfilled
				case Some(updates) =>
					// Checks for updates to apply, based on the targeted region
					val (updatesToDelay, updatesToApply) = region match {
						// Case: Targeting a subregion => Applies only updates that overlap with the targeted region
						case Some(region) =>
							updates
								.divideBy { case (image, position) =>
									Bounds(position, image.size).overlapsWith(region)
								}
								.toTuple
						// Case: Targeting the whole component region => Applies all updates
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
					
				// Case: There were too many updates => Buffers the whole component image
				case None =>
					val newImage = componentImage
					bufferPointer.setOne(newImage)
					newImage -> Some(Empty)
			}
		}
		// Case: Whole component was repainted => Clears queued updates
		else {
			queuePointer.update { case (processing, _) => processing -> Map() }
			queuedUpdatesPointer.setOne(Empty)
			baseImage
		}
	}
	
	// Pass None if whole component should be painted
	private def paintQueue(first: Option[Bounds]) = paint { drawer =>
		first match {
			case Some(region) => paintArea(drawer, region)
			case None => paintWith(drawer)
		}
		unqueue.foreach { paintArea(drawer, _) }
	}
	private def paintArea(drawer: Drawer, region: Bounds) = {
		// Buffers the targeted area
		val buffered = bufferArea(region)
		// Draws the buffered area using the drawer
		drawer.clippedToBounds(region).use { d => buffered.value.drawWith(d, component.position + region.position) }
	}
	
	/**
	 * Buffers an area, so that it will be ready for the next (full) buffer update
	 * @param region Targeted region
	 * @return A lazily initialized view of the visualized targeted region.
	 *         Not initialized, if buffering was not applied (in case of too many updates)
	 */
	private def bufferArea(region: Bounds) = {
		// Draws the component region to a separate image
		val buffered = Lazy {
			val base = component.regionToImage(region)
			background match {
				case Some(bg) => base.withBackground(bg)
				case None => base
			}
		}
		// Queues the buffer to be drawn when component will be fully painted next time
		queuedUpdatesPointer.update { _.flatMap { queue =>
			// If the queue reaches maximum size, invalidates it
			if (queue.size < maxQueueSize)
				Some(OptimizedIndexedSeq.concat(
					queue.view.filterNot { case (image, position) => region.contains(Bounds(position, image.size)) },
					Single(buffered.value -> region.position)
				))
			else
				None
		} }
		buffered
	}
	
	private def paint(f: Drawer => Unit) = {
		// Painting may be delayed
		delayPaintingFlag.onceNotSet {
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
	}
	// Doesn't perform any preparation, as in paint
	private def draw(f: Drawer => Unit) = {
		// Paints using the component's own graphics instance (which may not be available)
		Option(jComponent.getGraphics).foreach { g => Drawer(g.asInstanceOf[Graphics2D]).use(f) }
		if (syncAfterDraw)
			Try { Toolkit.getDefaultToolkit.sync() }
	}
}
