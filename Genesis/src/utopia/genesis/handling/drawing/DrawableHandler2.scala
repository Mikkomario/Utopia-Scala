package utopia.genesis.handling.drawing

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Priority2.Normal
import utopia.genesis.graphics.{DrawLevel2, DrawOrder, DrawSettings, Drawer, PaintManager2, Priority2, StrokeSettings}
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.handling.event.keyboard.Key.FunctionKey
import utopia.genesis.handling.event.keyboard.{KeyStateListener2, KeyboardEvents}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}
import utopia.genesis.image.MutableImage
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

object DrawableHandler2 extends FromCollectionFactory[Drawable2, DrawableHandler2]
{
	// IMPLEMENTED  ----------------------
	
	override def from(items: IterableOnce[Drawable2]): DrawableHandler2 = apply(items)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param items Drawable items to place in this handler, initially
	  * @param drawOrder Draw order used when using this handler as a Drawable item
	  * @return A new handler with the specified items
	  */
	def apply(items: IterableOnce[Drawable2], drawOrder: DrawOrder = DrawOrder.default) =
		new DrawableHandler2(drawOrder, items)
	
	/**
	  * @param drawOrder Draw order to use for the resulting handler(s), when they're used as Drawable items
	  * @return A factory for constructing handlers with that draw order
	  */
	def apply(drawOrder: DrawOrder) = DrawableHandlerFactory(drawOrder)
		
	
	// NESTED   --------------------------
	
	case class DrawableHandlerFactory(drawOrder: DrawOrder = DrawOrder.default)
		extends FromCollectionFactory[Drawable2, DrawableHandler2]
	{
		// IMPLEMENTED  ------------------
		
		override def from(items: IterableOnce[Drawable2]): DrawableHandler2 = apply(items)
		
		
		// OTHER    ----------------------
		
		/**
		  * @param items Items to place on this handler, initially
		  * @return A handler managing the specified items
		  */
		def apply(items: IterableOnce[Drawable2]) = DrawableHandler2(items, drawOrder)
	}
}

/**
  * Manages a set of drawable items and handles their paint operations
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
// TODO: Add FPS limiting
class DrawableHandler2(override val drawOrder: DrawOrder = DrawOrder.default,
                       initialItems: IterableOnce[Drawable2] = Vector.empty)
	extends DeepHandler2[Drawable2](initialItems) with Drawable2 with PaintManager2
{
	// ATTRIBUTES   --------------------------
	
	implicit private val testDs: DrawSettings = StrokeSettings(Color.red)
	
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
	
	
	// IMPLEMENTED  --------------------------
	
	override def opaque = false
	
	override def drawBoundsPointer: Changing[Bounds] = _drawBoundsPointer.readOnly
	
	override protected def repaintListeners: Iterable[RepaintListener] = _repaintListeners
	
	override def repaint(region: Option[Bounds], priority: Priority2) = {
		region match {
			case Some(region) => layers.foreach { _.updateBuffer(region) }
			case None => layers.foreach { _.resetBuffer() }
		}
		requestParentRepaint(region, priority)
	}
	
	override def draw(drawer: Drawer, bounds: Bounds) = paintWith(drawer)
	override def paintWith(drawer: Drawer) = {
		drawer.clippingBounds match {
			case Some(clip) =>
				println(s"Painting clip: $clip")
				_paint(drawer, clip)
			case None =>
				println("Painting all")
				layers.reverseIterator.foreach { _.paintWith(drawer) }
		}
	}
	
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
	
	private def _paint(drawer: Drawer, region: Bounds) = {
		// Checks which layers to paint
		val paintRegions = coveringLayers
			.foldLeftIterator[Option[Bounds]](Some(region)) { (region, layer) => region.flatMap(layer.cover) }
			.takeWhile { _.isDefined }.toVector.flatten
		// Paints the targeted layers from bottom to top
		layers.zip(paintRegions).reverseIterator.foreach { case (layer, region) =>
			// TODO: Check whether there is a more efficient way to do this than to just use clip
			layer.paintWith(drawer.clippedToBounds(region))
		}
	}
	
	private def updateDrawBounds() = _drawBoundsPointer.value = Bounds.around(layers.flatMap { _.drawBounds })
	
	
	// NESTED   ---------------------------
	
	private class Layer(level: DrawLevel2)
	{
		// ATTRIBUTES   -------------------
		
		private val buffer = MutableImage.empty
		
		private val itemsPointer = groupedItemsPointer.map { _(level) }
		private val orderedItemsPointer = itemsPointer.map { _.sortBy { _.drawOrder.orderIndex } }
		
		val drawBoundsPointer = EventfulPointer(Bounds.zero)
		private val boundsListener = ChangeListener[Bounds] { e =>
			// Extends the draw bounds, if necessary
			extendDrawBounds(e.newValue)
			// Repaints the changed area
			repaint(Bounds.around(e.values))
		}
		
		
		// INITIAL CODE ---------------------
		
		updateDrawBounds()
		drawBoundsPointer.addContinuousListener { e =>
			println(e.newValue)
			e.oldValue.overlapWith(e.newValue) match {
				case Some(overlap) => buffer.changeSourceResolution(e.newValue.size, overlap - e.oldValue.position)
				case None => buffer.resetAndResize(e.newValue.size)
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
		
		def drawBounds = if (items.nonEmpty) Some(drawBoundsPointer.value) else None
		
		
		// OTHER    ---------------------
		
		def paintWith(drawer: Drawer) = {
			drawBounds.foreach { bounds =>
				buffer.drawWith(drawer, bounds.position)
				drawer.draw(bounds.shrunk(4.0))
			}
		}
		
		// TODO: Consider using a more optimized reset function
		def resetBuffer() = buffer.paintOver { drawer =>
			drawer.clear(Bounds(Point.origin, buffer.size))
			val translation = drawBoundsPointer.value.position
			orderedItemsPointer.value.foreach { d => d.draw(drawer, d.drawBounds - translation) }
		}
		def updateBuffer(region: Bounds) = {
			// Determines the targeted area within the buffer image
			val translation = drawBoundsPointer.value.position
			val relativeRegion = region - translation
			// Determines which items to re-draw
			val targetItems = orderedItemsPointer.value.map { a => a -> a.drawBounds }
				.filter { _._2.overlapsWith(region) }
			// Updates the buffer
			buffer.paintOver { drawer =>
				// Clears the previous drawings
				drawer.clear(relativeRegion)
				// Draws the targeted items
				if (targetItems.nonEmpty)
					drawer.clippedToBounds(relativeRegion).use { drawer =>
						targetItems.foreach { case (item, bounds) => item.draw(drawer, bounds - translation) }
					}
			}
		}
		
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
			updateBuffer(region)
			requestParentRepaint(Some(region), priority)
		}
		
		private def extendDrawBounds(newArea: Bounds) = drawBoundsPointer.update { current =>
			if (current.contains(newArea))
				current
			else
				Bounds.around(Pair(current, newArea))
		}
		
		private def updateDrawBounds() = drawBoundsPointer.value = Bounds.around(items.map { _.drawBounds })
		
		
		// NESTED   ---------------------
		
		private object LayerRepaintListener extends RepaintListener
		{
			override def repaint(item: Drawable2, subRegion: Option[Bounds], priority: Priority2) = {
				val drawBounds = item.drawBounds
				val repaintBounds = subRegion match {
					case Some(region) => region + drawBounds.position
					case None => drawBounds
				}
				Layer.this.repaint(repaintBounds, priority)
			}
		}
	}
}
