package utopia.genesis.handling.drawing

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Priority2.Normal
import utopia.genesis.graphics._
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}
import utopia.genesis.image.MutableImage
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import scala.annotation.unused

object DrawableHandler2
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A default-state factory for DrawableHandlers
	  */
	val factory = DrawableHandlerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: DrawableHandler2.type): DrawableHandlerFactory = factory
		
	
	// NESTED   --------------------------
	
	case class DrawableHandlerFactory(clipPointer: Option[Changing[Bounds]] = None,
	                                  visiblePointer: Changing[Boolean] = AlwaysTrue,
	                                  drawOrder: DrawOrder = DrawOrder.default)
		extends FromCollectionFactory[Drawable2, DrawableHandler2]
	{
		// IMPLEMENTED  ------------------
		
		override def from(items: IterableOnce[Drawable2]): DrawableHandler2 = apply(items)
		
		
		// OTHER    ----------------------
		
		/**
		  * @param p A pointer that determines the area that's visible at any time
		  * @return Copy of this factory that only draws within the area specified by that pointer
		  */
		def withClipPointer(p: Changing[Bounds]) = copy(clipPointer = Some(p))
		/**
		  * @param clipBounds The only area allowed to be drawn by this handler
		  * @return Copy of this factory that only draws to that area
		  */
		def clippedTo(clipBounds: Bounds) = withClipPointer(Fixed(clipBounds))
		
		/**
		  * @param p A pointer that determines when this handler may be drawn
		  * @return Copy of this factory that only allows the handlers to be drawn while the specified pointer
		  *         contains true.
		  */
		def withVisibilityPointer(p: Changing[Boolean]) = copy(visiblePointer = p)
		
		/**
		  * @param drawLevel Targeted drawing level
		  * @return Copy of this factory where the handler contents will be drawn on the specified level
		  *         (if used as a Drawable)
		  */
		def drawnTo(drawLevel: DrawOrder) = copy(drawOrder = drawLevel)
		
		/**
		  * @param items Items to place on this handler, initially
		  * @return A handler managing the specified items
		  */
		def apply(items: IterableOnce[Drawable2]) = new DrawableHandler2(clipPointer, visiblePointer, drawOrder, items)
	}
}

/**
  * Manages a set of drawable items and handles their paint operations
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
// TODO: Add FPS limiting
class DrawableHandler2(clipPointer: Option[Changing[Bounds]] = None, visiblePointer: Changing[Boolean] = AlwaysTrue,
                       override val drawOrder: DrawOrder = DrawOrder.default,
                       initialItems: IterableOnce[Drawable2] = Vector.empty)
	extends DeepHandler2[Drawable2](initialItems, visiblePointer) with Drawable2 with PaintManager2
{
	// ATTRIBUTES   --------------------------
	
	private val groupedItemsPointer = itemsPointer.readOnly
		.map { _.groupBy { _.drawOrder.level }.withDefaultValue(Vector.empty) }
	
	// Layers from top to bottom
	private val layers = DrawLevel2.values.reverse.map { new Layer(_) }
	private val coveringLayers = layers.dropRight(1)
	
	private var _repaintListeners = Vector.empty[RepaintListener]
	
	private val _drawBoundsPointer = EventfulPointer(Bounds.zero)
	private val updateDrawBoundsListener = ChangeListener.onAnyChange { updateDrawBounds() }
	
	
	// INITIAL CODE --------------------------
	
	layers.foreach { _.drawBoundsPointer.addListener(updateDrawBoundsListener) }
	updateDrawBounds()
	
	// Whenever clipping changes, updates the layer draw areas
	clipPointer.foreach { _.addListenerWhile(handleCondition) { _ =>
		layers.foreach { _.updateDrawBounds() }
	} }
	
	
	// IMPLEMENTED  --------------------------
	
	override def opaque = false
	override def drawBoundsPointer: Changing[Bounds] = _drawBoundsPointer.readOnly
	
	override protected def repaintListeners: Iterable[RepaintListener] = _repaintListeners
	
	override def repaint(region: Option[Bounds], priority: Priority2) = clip(region).foreach { region =>
		region match {
			case Some(region) => layers.foreach { _.queueBufferUpdate(region, priority) }
			case None => layers.foreach { _.resetBuffer() }
		}
		requestParentRepaint(region, priority)
	}
	
	override def draw(drawer: Drawer, bounds: Bounds) =
		_paint(drawer, (bounds.position - drawBounds.position).toVector)
	override def paintWith(drawer: Drawer) = _paint(drawer)
	
	// TODO: Optimize at some point
	override def shift(originalArea: Bounds, transition: Vector2D) = {
		val totalArea = Bounds.around(Pair(originalArea, originalArea + transition))
		repaint(Some(totalArea))
	}
	
	override def addRepaintListener(listener: RepaintListener): Unit = {
		if (!_repaintListeners.contains(listener))
			_repaintListeners :+= listener
	}
	override def removeRepaintListener(listener: RepaintListener): Unit =
		_repaintListeners = _repaintListeners.filterNot { _ == listener }
	
	override protected def asHandleable(item: Handleable2) = item match {
		case d: Drawable2 => Some(d)
		case _ => None
	}
	
	
	// OTHER    ---------------------------
	
	private def requestParentRepaint(subRegion: Option[Bounds], priority: Priority2 = Normal) =
		super[Drawable2].repaint(subRegion, priority)
	
	private def _paint(drawer: Drawer, translation: Vector2D = Vector2D.zero) = {
		clip(drawer.clippingBounds).foreach {
			case Some(clip) =>
				// Checks which layers to paint
				val paintRegions = coveringLayers
					.foldLeftIterator[Option[Bounds]](Some(clip)) { (region, layer) => region.flatMap(layer.cover) }
					.takeWhile { _.isDefined }.toVector.flatten
				// Paints the targeted layers from bottom to top
				layers.zip(paintRegions).reverseIterator
					.foreach { case (layer, region) => layer.paintWith(drawer, Some(region), translation) }
			case None => layers.reverseIterator.foreach { _.paintWith(drawer, translation = translation) }
		}
	}
	
	private def updateDrawBounds() = _drawBoundsPointer.value = Bounds.around(layers.flatMap { _.drawBounds })
	
	// Determines the targeted region after clipping
	// Contains None if repaint is not needed (i.e. outside of clip)
	// Contains Some(Some) if a sub-region should be repainted
	// Contains Some(None) if everything should be repainted
	private def clip(region: Option[Bounds]) = clipPointer match {
		case Some(clipPointer) =>
			region match {
				case Some(region) => region.overlapWith(clipPointer.value).map { Some(_) }
				case None => Some(Some(clipPointer.value))
			}
		case None => Some(region)
	}
	private def clip(region: Bounds) = clipPointer match {
		case Some(clipPointer) => region.overlapWith(clipPointer.value)
		case None => Some(region)
	}
	
	
	// NESTED   ---------------------------
	
	private class Layer(level: DrawLevel2)
	{
		// ATTRIBUTES   -------------------
		
		private val buffer = MutableImage.empty
		// Contains the queued area to repaint
		private val queuedBufferUpdatePointer = Pointer.empty[Bounds]()
		
		private val itemsPointer = groupedItemsPointer.map { _(level) }
		private val orderedItemsPointer = itemsPointer.map { _.sortBy { _.drawOrder.orderIndex } }
		
		private var extensionsCount = 0
		
		val drawBoundsPointer = EventfulPointer.empty[Bounds]()
		private val boundsListener = ChangeListener[Bounds] { e =>
			// Extends the draw bounds, if necessary
			if (extensionsCount > 100) {
				extensionsCount = 0
				updateDrawBounds()
			}
			else {
				extensionsCount += 1
				extendDrawBounds(e.newValue)
			}
			// Repaints the changed area
			repaint(Bounds.around(e.values).round)
		}
		
		
		// INITIAL CODE ---------------------
		
		updateDrawBounds()
		drawBoundsPointer.addContinuousListener { e =>
			e.newValue match {
				case Some(newBounds) =>
					e.oldValue match {
						// Case: Change in bounds => Checks for overlap
						case Some(oldBounds) =>
							oldBounds.overlapWith(newBounds) match {
								// Case: Overlap => Resizes image, keeping image data
								case Some(overlap) =>
									accessBuffer { buffer =>
										dequeueBufferUpdates(buffer)
										buffer.changeSourceResolution(newBounds.size, overlap - oldBounds.position)
									}
								// Case: No overlap => Generates a new image
								case None => resetAndResizeBuffer(newBounds.size)
							}
						// Case: Acquired bounds => Generates a new image
						case None => resetAndResizeBuffer(newBounds.size)
					}
				// Case: Not drawn any more => Discards buffered image information
				case None => resetAndResizeBuffer()
			}
		}
		
		itemsPointer.addContinuousListenerAndSimulateEvent(Vector.empty) { e =>
			val (changes, _) = e.values.separateMatching
			// Case: Items removed => Stops listening on them
			changes.first.foreach { a =>
				a.removeRepaintListener(LayerRepaintListener)
				a.drawBoundsPointer.removeListener(boundsListener)
			}
			// Case: Items added => Starts listening and paints them, also
			changes.second.foreach { a =>
				a.drawBoundsPointer.addListener(boundsListener)
				a.addRepaintListener(LayerRepaintListener)
			}
			// Repaints the changed area
			updateDrawBounds()
			repaint(Bounds.around(changes.flatMap { _.map { _.drawBounds } }))
		}
		
		
		// COMPUTED ---------------------
		
		def items = itemsPointer.value
		
		def drawBounds = if (items.nonEmpty) drawBoundsPointer.value else None
		
		
		// OTHER    ---------------------
		
		def updateDrawBounds() = drawBoundsPointer.value = clip(Bounds.around(items.map { _.drawBounds }))
		
		// Assumes that clipping is already applied when selecting subRegion
		def paintWith(drawer: Drawer, subRegion: Option[Bounds] = None, translation: Vector2D = Vector2D.zero) =
			drawBounds.foreach { bounds =>
				// Position where the top left corner of the painted image will be placed
				val targetPosition = bounds.position + translation
				// Makes sure the buffer is up-to-date
				accessBuffer { buffer =>
					dequeueBufferUpdates(buffer)
					subRegion match {
						// Case: Drawing a sub-region => Only paints part of the buffer image
						case Some(region) =>
							bounds.overlapWith(region).foreach { overlap =>
								if (overlap == bounds)
									buffer.drawWith(drawer, targetPosition)
								else {
									val relativeArea = overlap - bounds.position
									buffer.drawSubImageWith(drawer, relativeArea, targetPosition)
								}
							}
						// Case: Drawing the whole image
						case None => buffer.drawWith(drawer, targetPosition)
					}
				}
			}
		
		// TODO: Consider using a more optimized reset function
		def resetBuffer() = {
			queuedBufferUpdatePointer.clear()
			clipPointer match {
				// Case: Clipping applied => Repaints the clip area
				case Some(clipPointer) =>
					drawBounds.flatMap { _.overlapWith(clipPointer.value) }
						.foreach { region => accessBuffer { updateBuffer(_, region) } }
				// Case: No clipping => Repaints the whole draw bounds
				case None =>
					drawBounds.foreach { bounds =>
						accessBuffer { buffer =>
							buffer.paintOver { drawer =>
								drawer.clear(Bounds(Point.origin, buffer.size))
								val translation = bounds.position
								orderedItemsPointer.value.foreach { d => d.draw(drawer, d.drawBounds - translation) }
							}
						}
					}
			}
		}
		def queueBufferUpdate(region: Bounds, priority: Priority2) = {
			// Case: High priority => Pre-draws the buffer
			if (priority > Normal) {
				val actualRegion = queuedBufferUpdatePointer.pop() match {
					case Some(queued) => Bounds.around(Pair(queued, region))
					case None => region
				}
				clip(actualRegion).foreach { region => accessBuffer { updateBuffer(_, region) } }
			}
			// Case: Normal or low priority => Queues a buffer update
			else
				queuedBufferUpdatePointer.update {
					case Some(queued) => Some(Bounds.around(Pair(queued, region)))
					case None => Some(region)
				}
		}
		private def dequeueBufferUpdates(buffer: MutableImage) =
			queuedBufferUpdatePointer.pop().flatMap(clip).foreach { updateBuffer(buffer, _) }
		private def updateBuffer(buffer: MutableImage, region: Bounds) = {
			drawBounds.foreach { bounds =>
				// Determines the targeted area within the buffer image
				val translation = bounds.position
				val relativeRegion = region - translation
				// Determines which items to re-draw
				val targetItems = orderedItemsPointer.value.map { a => a -> a.drawBounds }
					.filter { _._2.overlapsWith(region) }
				
				// Updates the buffer
				buffer.paintOver { drawer =>
					// Clears the previous drawings
					drawer.clear(relativeRegion)
					// Draws the targeted items (uses clipping)
					drawer.clippedToBounds(relativeRegion).use { drawer =>
						targetItems.foreach { case (item, bounds) => item.draw(drawer, bounds - translation) }
					}
				}
			}
		}
		private def resetAndResizeBuffer(newSize: Size = Size.zero) = accessBuffer { _.resetAndResize(newSize) }
		
		/**
		  * Checks whether this layer fully or partially covers the specified region with opaque elements
		  * @param region Targeted region
		  * @return Some if the specified region is not covered, or is covered only partially.
		  *         Contains the remaining uncovered area.
		  *         None if the specified region is fully covered.
		  */
		def cover(region: Bounds) = {
			items.iterator
				.filter { _.opaque }
				// Reduces the target region one item at a time
				.foldLeftIterator[Option[Bounds]](Some(region)) { (region, item) =>
					region.flatMap { region =>
						// Compares item bounds with the region
						val itemArea = item.drawBounds
						val horizontalItemArea = itemArea.x
						val verticalItemArea = itemArea.y
						
						val containsLeft = horizontalItemArea.contains(region.leftX)
						lazy val containsRight = horizontalItemArea.contains(region.rightX)
						val containsTop = verticalItemArea.contains(region.topY)
						lazy val containsBottom = verticalItemArea.contains(region.bottomY)
						
						// Case: At least horizontal containment => Possible to reduce the area
						if (containsLeft && containsRight) {
							// Case: Covers the top => Checks whether fully covered
							if (containsTop) {
								// Case: Fully covered
								if (containsBottom)
									None
								// Case: Only covers the top => Reduces the region size
								else
									Some(region.mapY { _.withStart(verticalItemArea.end) })
							}
							// Case: Covers the bottom only => Reduces the region size
							else if (containsBottom)
								Some(region.mapY { _.withEnd(verticalItemArea.start) })
							// Case: No vertical overlap at the edges => No clipping
							else
								Some(region)
						}
						// Case: Vertical containment => Checks whether the area may be reduced
						else if (containsTop && containsBottom) {
							// Case: Covers the left side => Reduces region size
							if (containsLeft)
								Some(region.mapX { _.withStart(horizontalItemArea.end) })
							// Case: Covers the right side => Reduces the region size
							else if (containsRight)
								Some(region.mapX { _.withEnd(horizontalItemArea.start) })
							// Case: No horizontal overlap at the edges  => No clipping
							else
								Some(region)
						}
						// Case: No containment on any axis => No clipping
						else
							Some(region)
					}
				}
				.takeTo { _.isEmpty }
				.last
		}
		
		private def repaint(region: Bounds, priority: Priority2 = Normal) = {
			clip(region).foreach { clipped =>
				queueBufferUpdate(clipped, priority)
				requestParentRepaint(Some(clipped), priority)
			}
		}
		
		private def extendDrawBounds(newArea: Bounds) = drawBoundsPointer.update {
			case Some(current) =>
				// Case: Draw bounds don't extend with this update
				if (current.contains(newArea))
					Some(current)
				else
					Some(clip(newArea) match {
						// Case: Draw bounds extend => Updates the area
						case Some(newArea) => Bounds.around(Pair(current, newArea))
						// Case: Extension is out of clip area => Not applied
						case None => current
					})
			// Case: No draw bounds previously => Assigns new bounds, if they fit within the current clip zone
			case None => clip(newArea)
		}
		
		// Facilitates protected access to the buffer property
		private def accessBuffer[A](f: MutableImage => A): A = this.synchronized { f(buffer) }
		
		
		// NESTED   ---------------------
		
		private object LayerRepaintListener extends RepaintListener
		{
			override def repaint(item: Drawable2, subRegion: Option[Bounds], priority: Priority2) = {
				val drawBounds = item.drawBounds
				val repaintBounds = subRegion match {
					case Some(region) => region + drawBounds.position
					case None => drawBounds
				}
				Layer.this.repaint(repaintBounds.round, priority)
			}
		}
	}
}
