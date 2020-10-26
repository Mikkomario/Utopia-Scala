package utopia.reflection.container.swing

import java.awt.event.KeyEvent
import java.awt.{AWTKeyStroke, Container, Graphics, KeyboardFocusManager}
import java.util

import javax.swing.{JComponent, JPanel}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.VolatileOption
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.image.Image
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.{Background, Foreground, Normal}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.component.reach.wrapper.ComponentCreationResult
import utopia.reflection.component.swing.template.{JWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.ReachFocusManager

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

object ReachCanvas
{
	/**
	  * Creates a new set of canvas with a reach component in them
	  * @param content Function for producing the content once parent hierarchy is available
	  * @tparam C Type of created canvas content
	  * @return A set of canvas with the content inside them and the produced canvas content as well
	  */
	def apply[C <: ReachComponentLike, R](content: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		val contentPromise = Promise[ReachComponentLike]()
		val canvas = new ReachCanvas(contentPromise.future)
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
class ReachCanvas private(contentFuture: Future[ReachComponentLike]) extends JWrapper with Stackable
	with AwtContainerRelated with SwingComponentRelated with CustomDrawable
{
	// ATTRIBUTES	---------------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	private var layoutUpdateQueue = VolatileList[Seq[ReachComponentLike]]()
	private var updateFinishedQueue = VolatileList[() => Unit]()
	private var buffer = Image.empty
	
	private val panel = new CustomDrawPanel()
	private val repaintNeed = VolatileOption[RepaintNeed](Full)
	
	/**
	  * Object that manages focus between the components in this canvas element
	  */
	val focusManager = new ReachFocusManager(panel)
	
	private val _attachmentPointer = new PointerWithEvents(false)
	
	
	// INITIAL CODE	---------------------------
	
	_attachmentPointer.addListener { event =>
		// When attached to the stack hierarchy, makes sure to update immediate content layout and repaint this component
		if (event.newValue)
		{
			repaintNeed.setOne(Full)
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
	addResizeListener { event =>
		currentContent.foreach { _.size = event.newSize }
		if (event.newSize.isPositive)
			repaintNeed.setOne(Full)
	}
	
	// Listens to tabulator key events for manual focus handling
	addKeyStateListener(FocusKeyListener)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return A pointer to this canvas' stack hierarchy attachment status
	  */
	def attachmentPointer = _attachmentPointer.view
	
	private def currentContent = contentFuture.current.flatMap { _.toOption }
	
	
	// IMPLEMENTED	---------------------------
	
	override def isAttachedToMainHierarchy = _attachmentPointer.value
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		_attachmentPointer.value = newAttachmentStatus
	
	override def component: JComponent with Container = panel
	
	override def updateLayout() =
	{
		// Updates content layout
		val layoutUpdateQueues = layoutUpdateQueue.popAll()
		if (layoutUpdateQueues.nonEmpty)
			updateLayoutFor(layoutUpdateQueues.toSet, Set())
		
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
		repaintNeed.setOne(Full)
		component.repaint()
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
	  */
	def repaint(area: Bounds) =
	{
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
	}
	
	@tailrec
	private def updateLayoutFor(componentQueues: Set[Seq[ReachComponentLike]], sizeChangedChildren: Set[ReachComponentLike]): Unit =
	{
		// Updates the layout of the next layer (from top to bottom) components. Checks for size changes and
		// also updates the children of components which changed size during the layout update
		val nextSizeChangeChildren = (componentQueues.map { _.head } ++ sizeChangedChildren).flatMap { c =>
			val oldSize = c.size
			c.updateLayout()
			if (c.size != oldSize)
				c.children
			else
				None
		}
		// Moves to the next layer of components, if there is one
		val remainingQueues = componentQueues.filter { _.size > 1 }
		if (remainingQueues.nonEmpty || nextSizeChangeChildren.nonEmpty)
			updateLayoutFor(remainingQueues.map { _.tail }, nextSizeChangeChildren)
	}
	
	
	// NESTED	------------------------------
	
	private object HierarchyConnection extends ComponentHierarchy
	{
		override def parent = Left(ReachCanvas.this)
		
		override def linkPointer = attachmentPointer
		
		override def isThisLevelLinked = isLinked
	}
	
	private class CustomDrawPanel extends JPanel(null)
	{
		// INITIAL CODE	---------------------
		
		setOpaque(false)
		setBackground(Color.black.toAwt)
		
		// Makes this canvas element focusable and disables the default focus traversal keys
		setFocusable(true)
		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		
		
		// IMPLEMENTED	----------------------
		
		override def paintComponent(g: Graphics) =
		{
			// Checks image buffer status first
			repaintNeed.pop().foreach {
				// Case: Completely repaints the buffer image
				case Full => buffer = Image.paint(ReachCanvas.this.size) { drawer => paintWith(drawer, None) }
				// Case: Repaints a portion of the image
				case Partial(area) =>
					buffer = Image.paint(ReachCanvas.this.size) { drawer =>
					buffer.drawWith(drawer)
					paintWith(drawer, Some(area))
				}
			}
			
			// Paints the buffered image
			Drawer.use(g) { drawer => buffer.drawWith(drawer) }
		}
		
		// Never paints children (because won't have any children)
		override def paintChildren(g: Graphics) = ()
		
		
		// OTHER	-----------------------------
		
		private def paintWith(drawer: Drawer, area: Option[Bounds]) =
		{
			// Draws background, if defined
			lazy val fullDrawBounds = drawBounds
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
		}
	}
	
	
	// NESTED	---------------------------------
	
	private sealed trait RepaintNeed
	private case object Full extends RepaintNeed
	private case class Partial(area: Bounds) extends RepaintNeed
	
	private object FocusKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	------------------------
		
		// Only listens to tabulator presses
		override val keyStateEventFilter = KeyStateEvent.keyFilter(KeyEvent.VK_TAB)
		
		
		// IMPLEMENTED	-----------------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			// Moves the focus forwards or backwards
			val direction = if (event.keyStatus(KeyEvent.VK_SHIFT)) Negative else Positive
			focusManager.moveFocus(direction)
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = focusManager.hasFocus
	}
}
