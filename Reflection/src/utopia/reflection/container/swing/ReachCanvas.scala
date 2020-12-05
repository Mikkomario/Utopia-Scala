package utopia.reflection.container.swing

import java.awt.event.KeyEvent
import java.awt.{AWTKeyStroke, Container, Graphics, KeyboardFocusManager}
import java.util
import javax.swing.{JComponent, JPanel}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.event.{KeyStateEvent, MouseButtonStateEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.{KeyStateListener, MouseMoveListener}
import utopia.genesis.image.Image
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.{Bounds, Point, Vector2D}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.color.ColorShade.Dark
import utopia.reflection.color.ColorShadeVariant
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.component.reach.wrapper.ComponentCreationResult
import utopia.reflection.component.swing.template.{JWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.cursor.{CursorSet, ReachCursorManager}
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.{Priority, ReachFocusManager, RealTimeReachPaintManager}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}

object ReachCanvas
{
	/**
	  * Creates a new set of canvas with a reach component in them
	  * @param cursors Cursors to use inside this component. None if this component shouldn't affect the cursor drawing.
	  * @param content Function for producing the content once parent hierarchy is available
	  * @tparam C Type of created canvas content
	  * @return A set of canvas with the content inside them and the produced canvas content as well
	  */
	def apply[C <: ReachComponentLike, R](cursors: Option[CursorSet] = None)
										 (content: ComponentHierarchy => ComponentCreationResult[C, R])
										 (implicit exc: ExecutionContext) =
	{
		val contentPromise = Promise[ReachComponentLike]()
		val canvas = new ReachCanvas(contentPromise.future, cursors)
		val newContent = content(canvas.HierarchyConnection)
		contentPromise.success(newContent.component)
		newContent in canvas
	}
}

/**
  * The component that connects a reach component hierarchy to the swing component hierarchy
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
class ReachCanvas private(contentFuture: Future[ReachComponentLike], cursors: Option[CursorSet])
						 (implicit exc: ExecutionContext)
	extends JWrapper with Stackable with AwtContainerRelated with SwingComponentRelated with CustomDrawable
{
	// ATTRIBUTES	---------------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	private var layoutUpdateQueue = VolatileList[Seq[ReachComponentLike]]()
	private var updateFinishedQueue = VolatileList[() => Unit]()
	// private var buffer = Image.empty
	
	private val panel = new CustomDrawPanel()
	// private val repaintNeed = VolatileOption[RepaintNeed](Full)
	
	/**
	  * Object that manages focus between the components in this canvas element
	  */
	val focusManager = new ReachFocusManager(panel)
	private val painterPromise = contentFuture.map { c => RealTimeReachPaintManager(c) /*{
		cursorPainter.flatMap { _.cursor } } { cursorPainter.map { _.cursorBounds } }*/ }
	/**
	  * Object that manages cursor display inside this canvas. None if cursor state is not managed in this canvas.
	  */
	val cursorManager = cursors.map { new ReachCursorManager(_) }
	private val cursorPainter = cursorManager.map { new CursorSwapper(_) }
	
	private val _attachmentPointer = new PointerWithEvents(false)
	
	
	// INITIAL CODE	---------------------------
	
	_attachmentPointer.addListener { event =>
		// When attached to the stack hierarchy, makes sure to update immediate content layout and repaint this component
		if (event.newValue)
		{
			// repaintNeed.setOne(Full)
			currentContent.foreach { content =>
				val branches = content.toTree.allBranches
				if (branches.isEmpty)
					layoutUpdateQueue :+= Vector(content)
				else
					layoutUpdateQueue ++= branches.map { content +: _ }
			}
		}
		fireStackHierarchyChangeEvent(event.newValue)
	}
	
	// Also requires a full repaint when size changes
	/*
	addResizeListener { event =>
		// currentContent.foreach { _.size = event.newSize }
		if (event.newSize.isPositive)
			currentPainter.foreach { _.repaintAll() }
		// repaintNeed.setOne(Full)
	}*/
	
	// Listens to tabulator key events for manual focus handling
	addKeyStateListener(FocusKeyListener)
	// Listens to mouse events for manual cursor drawing
	cursorPainter.foreach(addMouseMoveListener)
	// Also, disables the standard cursor drawing if manually handling cursor
	/*
	if (isManagingCursor)
		Try { Toolkit.getDefaultToolkit.createCustomCursor(
			new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
			new java.awt.Point(0, 0), "blank cursor") }.foreach { component.setCursor(_) }
	*/
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return A pointer to this canvas' stack hierarchy attachment status
	  */
	def attachmentPointer = _attachmentPointer.view
	
	/**
	  * @return Whether this component uses custom cursor painting features
	  */
	def isManagingCursor = cursorManager.nonEmpty
	
	private def currentContent = contentFuture.current.flatMap { _.toOption }
	
	private def currentPainter = painterPromise.current.flatMap { _.toOption }
	
	
	// IMPLEMENTED	---------------------------
	
	override def isAttachedToMainHierarchy = _attachmentPointer.value
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		_attachmentPointer.value = newAttachmentStatus
	
	override def component: JComponent with Container = panel
	
	override def updateLayout() =
	{
		// Updates content size
		val contentSizeChanged = currentContent match
		{
			case Some(content) =>
				val requiresSizeUpdate = content.size != size
				if (requiresSizeUpdate)
					content.size = size
				requiresSizeUpdate
			case None => false
		}
		
		// Updates content layout
		val layoutUpdateQueues = layoutUpdateQueue.popAll()
		val sizeChangeTargets: Set[ReachComponentLike] =
		{
			if (contentSizeChanged)
				currentContent.toSet
			else
				Set()
		}
		/*
		println()
		println(s"Content update (size changed = $contentSizeChanged)")
		println(s"${layoutUpdateQueues.size} update queues:")
		layoutUpdateQueues.foreach { q => println(s"\t- [${q.map { _.getClass.getSimpleName }.mkString(" -> ")}]") }
		 */
		if (layoutUpdateQueues.nonEmpty)
			updateLayoutFor(layoutUpdateQueues.toSet, sizeChangeTargets)
		
		// Performs the queued tasks
		updateFinishedQueue.popAll().foreach { _() }
	}
	
	override def stackSize = currentContent match
	{
		case Some(content) => content.stackSize
		case None => StackSize.any
	}
	
	override def resetCachedSize() = currentContent.foreach { _.resetCachedSize() }
	
	override val stackId = hashCode()
	
	override def drawBounds = Bounds(Point.origin, size)
	
	override def repaint() =
	{
		currentPainter.foreach { _.repaintAll() }
		/*
		repaintNeed.setOne(Full)
		component.repaint()
		 */
	}
	
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) =
	{
		super.distributeMouseButtonEvent(event) match
		{
			case Some(consumed) =>
				val newEvent = event.consumed(consumed)
				currentContent.foreach { _.distributeMouseButtonEvent(newEvent) }
				Some(consumed)
			case None => currentContent.flatMap { _.distributeMouseButtonEvent(event) }
		}
	}
	
	override def distributeMouseMoveEvent(event: MouseMoveEvent) =
	{
		super.distributeMouseMoveEvent(event)
		currentContent.foreach { _.distributeMouseMoveEvent(event) }
	}
	
	// TODO: WET WET
	override def distributeMouseWheelEvent(event: MouseWheelEvent) =
	{
		super.distributeMouseWheelEvent(event) match
		{
			case Some(consumed) =>
				val newEvent = event.consumed(consumed)
				currentContent.foreach { _.distributeMouseWheelEvent(newEvent) }
				Some(consumed)
			case None => currentContent.flatMap { _.distributeMouseWheelEvent(event) }
		}
	}
	
	// OTHER	------------------------------
	
	/**
	  * Revalidates this component, queueing some component layout updates to be done afterwards
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  */
	def revalidate(updateComponents: Seq[ReachComponentLike]): Unit =
	{
		val trueQueue = updateComponents.dropWhile { _ == this }
		if (trueQueue.nonEmpty)
			layoutUpdateQueue :+= trueQueue
		revalidate()
	}
	
	/**
	  * Revalidates this component's layout. Calls the specified function when whole component layout has been updated.
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  * @param f A function called after layout has been updated.
	  */
	def revalidateAndThen(updateComponents: Seq[ReachComponentLike])(f: => Unit) =
	{
		// Queues the action
		updateFinishedQueue :+= (() => f)
		// Queues revalidation
		revalidate(updateComponents)
	}
	
	/**
	  * Repaints a part of this canvas
	  * @param area Area to paint again
	  * @param priority Priority to use for this repaint. The high level priority areas are painted first.
	  */
	def repaint(area: Bounds, priority: Priority = Priority.Normal) =
	{
		currentPainter.foreach { _.repaintRegion(area, priority) }
		/*
		repaintNeed.update
		{
			case Some(old) =>
				old match
				{
					case Full => Some(old)
					case Partial(oldArea) => Some(Partial(Bounds.around(Vector(oldArea, area))))
				}
			case None => Some(Partial(area))
		}
		component.repaint(new Rectangle(area.x.toInt, area.y.toInt,area.width.toInt + 1,area.height.toInt + 1))
		 */
	}
	
	/**
	  * Shifts a painted region inside these canvases
	  * @param originalArea The area to shift (relative to this canvas' top left corner)
	  * @param translation Translation vector to apply to the area
	  */
	def shiftArea(originalArea: Bounds, translation: Vector2D) =
		currentPainter.foreach { _.shift(originalArea, translation) }
	
	@tailrec
	private def updateLayoutFor(componentQueues: Set[Seq[ReachComponentLike]], sizeChangedChildren: Set[ReachComponentLike]): Unit =
	{
		/*
		println("--------------")
		println(s"Updating based on size changes (${sizeChangedChildren.size}): [${sizeChangedChildren.map { _.getClass.getSimpleName }.mkString(", ")}]")
		println(s"Updating based on revalidation (${componentQueues.size}): [${componentQueues.flatMap { _.headOption }.map { _.getClass.getSimpleName }.mkString(", ")}]")
		*/
		// Updates the layout of the next layer (from top to bottom) components. Checks for size changes and
		// also updates the children of components which changed size during the layout update
		val nextSizeChangeChildren = (componentQueues.map { _.head } ++ sizeChangedChildren).flatMap { c =>
			val oldChildSizes = c.children.map { c => c -> c.size }
			c.updateLayout()
			oldChildSizes.flatMap { case (child, oldSize) => if (child.size != oldSize) Some(child) else None }
		}
		// Moves to the next layer of components, if there is one
		val remainingQueues = componentQueues.filter { _.size > 1 }
		if (remainingQueues.nonEmpty || nextSizeChangeChildren.nonEmpty)
			updateLayoutFor(remainingQueues.map { _.tail }, nextSizeChangeChildren)
	}
	
	
	// NESTED	------------------------------
	
	private sealed trait RepaintNeed
	private case object Full extends RepaintNeed
	private case class Partial(area: Bounds) extends RepaintNeed
	
	private object HierarchyConnection extends ComponentHierarchy
	{
		override def parent = Left(ReachCanvas.this)
		
		override def linkPointer = attachmentPointer
		
		override def isThisLevelLinked = isLinked
	}
	
	private class CustomDrawPanel extends JPanel(null)
	{
		// INITIAL CODE	---------------------
		
		setOpaque(true)
		setBackground(Color.black.toAwt)
		
		// Makes this canvas element focusable and disables the default focus traversal keys
		setFocusable(true)
		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		
		
		// IMPLEMENTED	----------------------
		
		override def paint(g: Graphics) = paintComponent(g)
		
		override def paintComponent(g: Graphics) =
		{
			currentPainter.foreach { p => Drawer.use(g)(p.paintWith) }
			/*
			// Checks image buffer status first
			repaintNeed.pop().foreach {
				// Case: Completely repaints the buffer image
				case Full => buffer = Image.paint(ReachCanvas.this.size) { drawer => paintWith(drawer, None) }
				// Case: Repaints a portion of the image
				case Partial(area) =>
					buffer = Image.paint(ReachCanvas.this.size) { drawer =>
					buffer.drawWith(drawer)
					paintWith(drawer, Some(area.ceil))
				}
			}
			
			// Paints the buffered image (and possibly the mouse cursor over it)
			Drawer.use(g) { drawer =>
				buffer.drawWith(drawer)
				cursorPainter.foreach { _.paintWith(drawer) }
			}*/
		}
		
		// Never paints children (because won't have any children)
		override def paintChildren(g: Graphics) = ()
		
		
		// OTHER	-----------------------------
		
		/*
		private def paintWith(drawer: Drawer, area: Option[Bounds]) =
		{
			// Draws background, if defined
			lazy val fullDrawBounds = drawBounds
			// TODO: Remove background drawing?
			if (!ReachCanvas.this.isTransparent)
				drawer.onlyFill(background).draw(area.getOrElse(fullDrawBounds))
			
			val drawersPerLayer = customDrawers.groupBy { _.drawLevel }
			// Draws background custom drawers and then normal custom drawers, if defined
			val backgroundAndNormalDrawers = drawersPerLayer.getOrElse(Background, Vector()) ++
				drawersPerLayer.getOrElse(Normal, Vector())
			if (backgroundAndNormalDrawers.nonEmpty)
			{
				val d = area.map(drawer.clippedTo).getOrElse(drawer)
				backgroundAndNormalDrawers.foreach { _.draw(d, fullDrawBounds) }
			}
			
			// Draws component content
			currentContent.foreach { _.paintWith(drawer, area) }
			
			// Draws foreground, if defined
			drawersPerLayer.get(Foreground).foreach { drawers =>
				val d = area.map(drawer.clippedTo).getOrElse(drawer)
				drawers.foreach { _.draw(d, fullDrawBounds) }
			}
		}*/
	}
	
	private object FocusKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	------------------------
		
		// Only listens to tabulator presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.keyFilter(KeyEvent.VK_TAB)
		
		
		// IMPLEMENTED	-----------------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			// Moves the focus forwards or backwards
			val direction = if (event.keyStatus.shift) Negative else Positive
			focusManager.moveFocus(direction)
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = focusManager.hasFocus
	}
	
	// TODO: Remove or drastically improve this implementation
	/*
	private class CursorPainter(cursorManager: ReachCursorManager) extends MouseMoveListener with Handleable
	{
		// ATTRIBUTES	-----------------------------
		
		private val projectionLength = 0.065.seconds
		private val cursorMovementTracker = new VelocityTracker2D(projectionLength, 0.01.seconds)
		
		private var lastMoveEventTime = Instant.now() - projectionLength
		private var lastMousePosition = Point.origin
		private var lastDrawnCursor: Option[(Point, Image)] = None
		
		private val maxCursorBounds = cursorManager.cursors.expectedMaxBounds.enlarged(Size(24, 24))
		
		
		// COMPUTED	---------------------------------
		
		def cursorPosition = cursorMovementTracker.projectedStatus.position
		
		
		// IMPLEMENTED	-----------------------------
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			// Records both the current and previous positions
			val eventTime = Instant.now()
			lastMoveEventTime = eventTime
			val lastPosition = event.previousMousePosition - position
			val newPosition = event.mousePosition - position
			lastMousePosition = newPosition
			// cursorMovementTracker.recordPosition(lastPosition, eventTime - event.duration)
			cursorMovementTracker.recordPosition(newPosition.toVector, eventTime)
			
			if (bounds.contains(event.mousePosition) || bounds.contains(event.previousMousePosition))
			{
				val projectedPosition = (event.mousePosition + event.velocity.over(projectionLength)) - position
				component.repaint(Bounds.around(Vector(lastPosition, projectedPosition).map { maxCursorBounds.translated(_) }).toAwt)
				// component.repaint()
			}
		}
		
		
		// OTHER	----------------------------------
		
		def paintWith(drawer: Drawer) =
		{
			// If the cursor stayed still, uses previously calculated information
			lazy val currentCursorPosition =
			{
				if (lastMoveEventTime < Instant.now() - projectionLength)
					lastMousePosition
				else
					cursorPosition.toPoint
			}
			lastDrawnCursor.filter { case (pos, _) => pos == lastMousePosition || (pos ~== currentCursorPosition) } match
			{
				case Some((lastPosition, lastImage)) => lastImage.drawWith(drawer, lastPosition)
				case None =>
					// Otherwise needs to recalculate the cursor style
					// println(currentCursorPosition)
					if (bounds.contains(currentCursorPosition + position))
					{
						val image = cursorManager.cursorAt(currentCursorPosition) { area =>
							// val luminance = buffer.pixelAt(area.center).luminosity
							val luminance = buffer.averageLuminosityOf(area)
							if (luminance >= 0.5) Light else Dark
						}
						lastDrawnCursor = Some(currentCursorPosition -> image)
						image.drawWith(drawer, currentCursorPosition)
					}
			}
		}
	}*/
	/*
	private class CursorPainter2(cursorManager: ReachCursorManager) extends MouseMoveListener with Handleable
	{
		// ATTRIBUTES	-----------------------------
		
		private val projectionLength = 0.065.seconds
		
		private var lastMousePosition = Point.origin
		private var lastDrawnCursor: Option[(Point, Image)] = None
		
		private val maxCursorBounds = cursorManager.cursors.expectedMaxBounds.enlarged(Size(24, 24))
		
		
		// COMPUTED	---------------------------------
		
		def cursorBounds = maxCursorBounds + lastMousePosition
		
		def cursor =
		{
			// Uses previously calculated data, if still effective
			val cached = lastDrawnCursor.filter { case (pos, _) => pos == lastMousePosition }
			if (cached.nonEmpty)
				cached
			else
			{
				// Needs to recalculate the cursor style
				if (bounds.contains(lastMousePosition + position))
				{
					val image = cursorManager.cursorImageAt(lastMousePosition) { area =>
						// FIXME: Luminance calculation doesn't work here
						// val luminance = buffer.averageLuminosityOf(area)
						val luminance = 1.0
						if (luminance >= 0.5) Light else Dark
					}
					val next = Some(lastMousePosition -> image)
					lastDrawnCursor = next
					next
				}
				else
					None
			}
		}
		
		
		// IMPLEMENTED	-----------------------------
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			val newPosition = event.mousePosition - position
			lastMousePosition = newPosition
			
			if (bounds.contains(event.mousePosition) || bounds.contains(event.previousMousePosition))
			{
				val projectedPosition = (event.mousePosition + event.velocity.over(projectionLength)) - position
				repaint(Bounds.around(Vector(event.previousMousePosition - position, projectedPosition)
					.map { maxCursorBounds.translated(_) }), VeryHigh)
				// component.repaint(Bounds.around(Vector(event.previousMousePosition - position, projectedPosition)
				// 	.map { maxCursorBounds.translated(_) }).toAwt)
				// component.repaint()
			}
		}
		
		
		// OTHER	----------------------------------
		
		def paintWith(drawer: Drawer) = cursor.foreach { case (position, image) => image.drawWith(drawer, position) }
	}*/
	
	private class CursorSwapper(cursorManager: ReachCursorManager) extends MouseMoveListener with Handleable
	{
		// ATTRIBUTES	-----------------------------
		
		private var lastMousePosition = Point.origin
		private var lastCursorImage: Option[Image] = None
		
		private val shadeCalculatorFuture = painterPromise.map[Bounds => ColorShadeVariant] { painter =>
			area => painter.averageShadeOf(area)
		}
		
		
		// COMPUTED	---------------------------------
		
		/*
		def cursor =
		{
			// Uses previously calculated data, if still effective
			val cached = lastCursor.filter { case (pos, _) => pos == lastMousePosition }
			if (cached.nonEmpty)
				cached
			else
			{
				// Needs to recalculate the cursor style
				if (bounds.contains(lastMousePosition + position))
				{
					val image = cursorManager.cursorImageAt(lastMousePosition) { area =>
						// FIXME: Luminance calculation doesn't work here
						// val luminance = buffer.averageLuminosityOf(area)
						val luminance = 1.0
						if (luminance >= 0.5) Light else Dark
					}
					val next = Some(lastMousePosition -> image)
					lastCursor = next
					next
				}
				else
					None
			}
		}*/
		
		
		// IMPLEMENTED	-----------------------------
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			val newPosition = event.mousePosition - position
			if (lastMousePosition != newPosition)
			{
				lastMousePosition = newPosition
				if (bounds.contains(event.mousePosition))
				{
					val newImage = cursorManager.cursorImageAt(newPosition) { area =>
						shadeCalculatorFuture.currentSuccess  match
						{
							case Some(calculate) => calculate(area)
							case None => Dark
						}
					}
					if (!lastCursorImage.contains(newImage))
					{
						lastCursorImage = Some(newImage)
						cursorManager.cursorForImage(newImage).foreach(component.setCursor)
					}
				}
			}
		}
	}
}
