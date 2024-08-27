package utopia.genesis.handling.drawing

import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.{Until, UntilNotified, WaitDuration}
import utopia.flow.async.process.{PostponingProcess, WaitTarget}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.Priority.{High, Normal, VeryLow}
import utopia.genesis.graphics._
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}
import utopia.genesis.image.MutableImage
import utopia.genesis.util.Fps
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import scala.annotation.unused
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object DrawableHandler
{
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: DrawableHandler.type)
	                            (implicit exc: ExecutionContext, log: Logger): DrawableHandlerFactory =
		DrawableHandlerFactory()
		
	
	// NESTED   --------------------------
	
	/**
	  * @param clipPointer Pointer that determines draw clipping area
	  * @param visiblePointer Pointer that determines when this handler may be drawn
	  * @param drawOrder Applied draw order when this handler is used as a Drawable instance
	  * @param exc Implicit execution context used when queing delayed repaints
	  * @param log Logging implementation for catching errors in queued repaints
	  */
	case class DrawableHandlerFactory(clipPointer: Option[Changing[Bounds]] = None,
	                                  visiblePointer: FlagLike = AlwaysTrue,
	                                  fpsLimits: Map[Priority, Fps] = Map(), preDrawPriority: Priority = High,
	                                  drawOrder: DrawOrder = DrawOrder.default)
	                                 (implicit exc: ExecutionContext, log: Logger)
		extends HandlerFactory[Drawable, DrawableHandler, DrawableHandlerFactory]
	{
		// ATTRIBUTES   ------------------
		
		override val condition = visiblePointer
		
		
		// IMPLEMENTED  ------------------
		
		override def usingCondition(newCondition: FlagLike): DrawableHandlerFactory = copy(visiblePointer = newCondition)
		
		override def apply(items: IterableOnce[Drawable]) =
			new DrawableHandler(clipPointer, visiblePointer, drawOrder, fpsLimits, preDrawPriority, items)
		
		
		// OTHER    ----------------------
		
		/**
		  * @param p A pointer that determines the area that's visible at any time.
		  *          This pointer will also dictate the draw bounds of this handler.
		  * @return Copy of this factory that only draws within the area specified by that pointer.
		  */
		def withClipPointer(p: Changing[Bounds]) = copy(clipPointer = Some(p))
		/**
		  * @param clipBounds The only area allowed to be drawn by this handler.
		  *                   This will also dictate the draw bounds of this handler.
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
		  * Overwrites this factory's current FPS limits.
		  * @param fpsLimits New FPS limits to assign.
		  *                  The keys are the priorities from which these limits apply.
		  *                  The values are the maximum FPS values allowed for those priorities.
		  *
		  *                  Not all keys need to be defined.
		  *                  Lower priority keys will be populated automatically with higher priority values
		  *                  if left empty.
		  *
		  * @return Copy of this factory that applies the specified limits
		  */
		def withFpsLimits(fpsLimits: Map[Priority, Fps]) = {
			// Case: No limits
			if (fpsLimits.isEmpty)
				copy(fpsLimits = fpsLimits)
			else {
				// Assigns the missing priorities
				val maxPrio = fpsLimits.keys.max
				val appliedLimits = maxPrio.lessIterator
					.foldLeftIterator(maxPrio -> fpsLimits(maxPrio)) { case ((_, higher), prio) =>
						// Uses assigned value if possible, otherwise copies higher priority value
						val appliedLimit = fpsLimits.getOrElse(prio, higher)
						prio -> appliedLimit
					}
					.toMap
				copy(fpsLimits = appliedLimits)
			}
		}
		/**
		  * Assigns a new FPS limit value
		  * @param limit Assigned FPS limit
		  * @param startingFrom Largest draw-priority for which this limit applies.
		  *                     Default = one priority lower than the (high) pre-draw priority.
		  * @return Copy of this factory that applies the specified FPS limit
		  */
		def withFpsLimit(limit: Fps, startingFrom: Priority = preDrawPriority.less) =
			mapFpsLimits { oldLimits =>
				oldLimits.map { case (prio, oldLimit) =>
					val appliedLimit = if (prio <= startingFrom) oldLimit min limit else oldLimit
					prio -> appliedLimit
				} ++ startingFrom.andSmaller.filterNot(oldLimits.contains).map { _ -> limit }
			}
		/**
		  * Modifies the applied FPS limits
		  * @param f A function which modifies FPS limits.
		  *          Priorities not listed by the function result will be automatically populated with higher
		  *          priority values, if applicable.
		  * @return Copy of this factory that uses modified FPS limits
		  */
		def mapFpsLimits(f: Mutate[Map[Priority, Fps]]) = withFpsLimits(f(fpsLimits))
		
		/**
		  * @param priority Priority that acts as the threshold for pre-drawing.
		  *                 Pre-drawing means that the requested repaints are buffered immediately,
		  *                 in order to possibly make the painting faster.
		  * @return Copy of this factory with the specified pre-draw setting
		  */
		def preDrawingAt(priority: Priority) = copy(preDrawPriority = priority)
		
		/**
		  * @param drawLevel Targeted drawing level
		  * @return Copy of this factory where the handler contents will be drawn on the specified level
		  *         (if used as a Drawable)
		  */
		def drawnTo(drawLevel: DrawOrder) = copy(drawOrder = drawLevel)
	}
}

/**
  * Manages a set of drawable items and handles their paint operations
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class DrawableHandler(clipPointer: Option[Changing[Bounds]] = None, visiblePointer: Changing[Boolean] = AlwaysTrue,
                      override val drawOrder: DrawOrder = DrawOrder.default, fpsLimits: Map[Priority, Fps] = Map(),
                      preDrawPriority: Priority = High, initialItems: IterableOnce[Drawable] = Empty)
                     (implicit exc: ExecutionContext, log: Logger)
	extends DeepHandler[Drawable](initialItems, visiblePointer) with Drawable with PaintManager
{
	// ATTRIBUTES   --------------------------
	
	private var _repaintListeners: Seq[RepaintListener] = Empty
	
	private val groupedItemsPointer = itemsPointer.readOnly
		.map { _.groupBy { _.drawOrder.level }.withDefaultValue(Empty) }
	
	// Layers from top to bottom
	private val layers = DrawLevel.values.reverse.map { new Layer(_) }
	private val coveringLayers = layers.dropRight(1)
	
	private val _drawBoundsPointer = clipPointer.toRight { EventfulPointer(Bounds.zero) }
	override val drawBoundsPointer: Changing[Bounds] = _drawBoundsPointer.either.readOnly
	
	// If used, contains: 1) Time to next repaint, 2) Repaint bounds, and 3) repaint priority
	// Contains None while no repaint has been requested
	private val queuedRepaintPointer = {
		if (fpsLimits.isEmpty)
			None
		else
			Some(VolatileOption[(WaitTarget, Option[Bounds], Priority)]((UntilNotified, None, VeryLow)))
	}
	private val repaintWaitPointer = queuedRepaintPointer.map { _.strongMap {
		case Some((time, _ , _)) => time
		case None => UntilNotified
	} }
	// Process that performs the delayed repaint
	private val repaintProcess = repaintWaitPointer.map { p =>
		PostponingProcess(p, shutdownReaction = Some(Cancel)) { _ =>
			queuedRepaintPointer.foreach { _.pop().foreach { case (_, region, priority) =>
				super[Drawable].repaint(region, priority)
			} }
		}
	}
	
	
	// INITIAL CODE --------------------------
	
	_drawBoundsPointer.leftOption.foreach { pointer =>
		val updateDrawBoundsListener = ChangeListener.onAnyChange { updateDrawBounds(pointer) }
		layers.foreach { _.drawBoundsPointer.addListener(updateDrawBoundsListener) }
		updateDrawBounds(pointer)
	}
	
	// Whenever clipping changes, updates the layer draw areas
	clipPointer.foreach { _.addListenerWhile(handleCondition) { _ =>
		layers.foreach { _.updateDrawBounds() }
	} }
	
	// Starts delayed repaint processing while handleable, if applicable
	repaintProcess.foreach { process =>
		handleCondition.addContinuousListener { e =>
			if (e.newValue)
				process.runAsync()
			else
				process.stopIfRunning()
		}
	}
	
	
	// IMPLEMENTED  --------------------------
	
	override def opaque = false
	
	override def repaintListeners: Iterable[RepaintListener] = _repaintListeners
	
	// Repainting is clipped and possibly delayed
	override def repaint(region: Option[Bounds], priority: Priority) = clip(region).foreach { region =>
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
	
	override protected def asHandleable(item: Handleable) = item match {
		case d: Drawable => Some(d)
		case _ => None
	}
	
	
	// OTHER    ---------------------------
	
	private def requestParentRepaint(subRegion: Option[Bounds], priority: Priority = Normal) = {
		// Checks whether an FPS-limit applies to this priority
		fpsLimits.get(priority) match {
			// Case: FPS limit applies => Queues the repaint asynchronously
			case Some(fpsLimit) =>
				queuedRepaintPointer.foreach { _.update {
					// Case: There was already a repaint queued => May update the queued repaint
					case Some((previousTime, previousRegion, previousPriority)) =>
						// Selects the earlier repaint time
						val newTime = previousTime.endTime match {
							case Some(previousEnd) => Until(previousEnd min (Now + fpsLimit.interval))
							case None => WaitDuration(fpsLimit.interval)
						}
						// Makes sure both regions are covered
						val newRegion = previousRegion
							.flatMap { r1 => subRegion.map { r2 => Bounds.around(Pair(r1, r2)) } }
						// Uses the highest available priority
						val newPriority = previousPriority max priority
						Some((newTime, newRegion, newPriority))
					// Case: No repaint was queued previously => Queues the new repaint
					case None => Some((WaitDuration(fpsLimit.interval), subRegion, priority))
				} }
			// Case: No FPS limit applies => Requests the repaint immediately
			case None => immediatelyRequestParentRepaint(subRegion, priority)
		}
	}
	private def immediatelyRequestParentRepaint(subRegion: Option[Bounds], priority: Priority = Normal) = {
		// Checks whether a repaint had been queued previously, and clears the queue
		val (appliedRegion, appliedPriority) = queuedRepaintPointer.flatMap { _.pop() } match {
			// Case: Repaint had been queued => Includes the queued area and uses the highest available priority
			case Some((_, queuedRegion, queuedPriority)) =>
				val combinedRegion = subRegion.flatMap { r1 => queuedRegion.map { r2 => Bounds.around(Pair(r1, r2)) } }
				val prio = priority max queuedPriority
				combinedRegion -> prio
			// Case: No repaint queued => Delivers requested repaint to parent
			case None => subRegion -> priority
		}
		super[Drawable].repaint(appliedRegion, appliedPriority)
	}
	
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
	
	// Accepts the mutable draw bounds -pointer
	private def updateDrawBounds(pointer: Pointer[Bounds]) =
		pointer.value = Bounds.around(layers.flatMap { _.drawBounds })
	
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
	
	private class Layer(level: DrawLevel)
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
		
		itemsPointer.addContinuousListenerAndSimulateEvent(Empty) { e =>
			val (changes, _) = e.values.separateMatching
			val updatedBounds = Bounds.around(changes.flatMap { _.map { _.drawBounds } })
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
			repaint(updatedBounds)
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
								if (overlap ~== bounds)
									buffer.drawWith(drawer, targetPosition)
								else {
									val relativeArea = (overlap - bounds.position).ceil // NB: Rounding might be unnecessary
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
		def queueBufferUpdate(region: Bounds, priority: Priority) = {
			// Case: High priority => Pre-draws the buffer
			if (priority >= preDrawPriority) {
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
				val relativeRegion = (region - translation).ceil // NB: Rounding here might be unnecessary
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
		private def resetAndResizeBuffer(newSize: Size = Size.zero) = {
			accessBuffer { _.resetAndResize(newSize) }
			// Repaints the whole canvas after resetting
			requestParentRepaint(clipPointer.map { _.value })
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
		
		private def repaint(region: Bounds, priority: Priority = Normal) = {
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
			override def repaint(item: Drawable, subRegion: Option[Bounds], priority: Priority) = {
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
