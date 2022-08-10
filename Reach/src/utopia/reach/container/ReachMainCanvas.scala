package utopia.reach.container

import utopia.flow.datastructure.immutable.Tree
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.event.{Consumable, ConsumeEvent, KeyStateEvent, MouseButtonStateEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{KeyStateListener, MouseMoveListener}
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.{ComponentCreationResult, ComponentWrapResult}
import utopia.reach.cursor.{CursorSet, ReachCursorManager}
import utopia.reach.focus.ReachFocusManager
import utopia.reflection.color.ColorShade.Dark
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.swing.template.{JWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.Never
import utopia.reflection.event.StackHierarchyListener
import utopia.paradigm.enumeration.Alignment

import java.awt.event.KeyEvent
import java.awt.{AWTKeyStroke, Container, Graphics, KeyboardFocusManager}
import java.util
import javax.swing.{JComponent, JPanel}
import scala.concurrent.{ExecutionContext, Future}


/**
  * The component that connects a reach component hierarchy to the swing component hierarchy. This component is
  * able to manage multiple canvases (windows) at once.
  * @author Mikko Hilpinen
  * @since 18.4.2021, v1.0
  */
// TODO: Continue working on or remove this class
// TODO: Stack hierarchy attachment link should be considered broken while this component is invisible or otherwise not shown
class ReachMainCanvas/* private(contentFuture: Future[ReachComponentLike], cursors: Option[CursorSet],
                              disableDoubleBufferingDuringDraw: Boolean = true, syncAfterDraw: Boolean = true,
                              focusEnabled: Boolean = true)
                             (implicit exc: ExecutionContext)
	extends JWrapper with Stackable with AwtContainerRelated with SwingComponentRelated with CustomDrawable
{
	// ATTRIBUTES	---------------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	private val panel = new CustomDrawPanel()
	
	val focusManager = new ReachFocusManager(panel)
	
	val cursorManager = cursors.map { new ReachCursorManager(_) }
	private val cursorPainter = cursorManager.map { new CursorSwapper(_) }
	
	private val _attachmentPointer = new PointerWithEvents(false)
	
	private val mainCanvas = new ReachCanvas2(contentFuture, attachmentPointer, disableDoubleBufferingDuringDraw,
		syncAfterDraw)(revalidate)
	private var windowTrees = Vector[Tree[ReachWindow]]()
	
	
	// INITIAL CODE	---------------------------
	
	_attachmentPointer.addListener { event =>
		// When attached to the stack hierarchy, makes sure to update immediate content layout and repaint this component
		if (event.newValue)
			mainCanvas.updateWholeLayout(size)
		// TODO: Update window layouts
		fireStackHierarchyChangeEvent(event.newValue)
	}
	
	// Listens to tabulator key events for manual focus handling
	if (focusEnabled)
		addKeyStateListener(FocusKeyListener)
	// Listens to mouse events for manual cursor drawing
	cursorPainter.foreach(addMouseMoveListener)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return A pointer to this canvas' stack hierarchy attachment status
	  */
	def attachmentPointer = _attachmentPointer.view
	
	/**
	  * @return Whether this component uses custom cursor painting features
	  */
	def isManagingCursor = cursorManager.nonEmpty
	
	private def canvases = mainCanvas +:
		windowTrees.flatMap { _.allContentIterator.map { _.canvas } }
	
	private def bottomToTopCanvases = mainCanvas +:
		windowTrees.flatMap { tree => tree +: tree.nodesBelowOrdered }.map { _.content.canvas }
	
	private def topToBottomCanvases =
		windowTrees.flatMap { tree => tree.nodesBelowOrdered.reverse :+ tree }.map { _.content.canvas } :+ mainCanvas
	
	
	// IMPLEMENTED	---------------------------
	
	override def isAttachedToMainHierarchy = _attachmentPointer.value
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		_attachmentPointer.value = newAttachmentStatus
	
	override def component: JComponent with Container = panel
	
	// TODO: Update main canvas & canvas windows
	override def updateLayout(): Unit =
	{
		// Updates main canvas bounds and layout
		mainCanvas.size = size
		mainCanvas.updateLayout()
		// TODO: Paint according to layout changes
		
		// TODO: Update window layouts also
	}
	
	// TODO: Take windows into account
	override def stackSize = mainCanvas.stackSize
	
	// TODO: Also reset window sizes
	override def resetCachedSize() = mainCanvas.resetCachedSize()
	
	override val stackId = hashCode()
	
	override def drawBounds = Bounds(Point.origin, size)
	
	// TODO: Paint main canvas (using that canvas' paint manager), then the windows
	override def repaint() = super[ReachCanvasLike].repaint()
	
	// TODO: Is the mouse position correct?
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) =
	{
		val baseEvent = super.distributeMouseButtonEvent(event) match
		{
			case Some(consumed) => event.consumed(consumed)
			case None => event
		}
		distributeConsumableEvent(baseEvent) { _.distributeMouseButtonEvent(_) }
	}
	
	override def distributeMouseMoveEvent(event: MouseMoveEvent) =
	{
		super.distributeMouseMoveEvent(event)
		canvases.foreach { _.distributeMouseMoveEvent(event) }
	}
	
	// TODO: WET WET
	override def distributeMouseWheelEvent(event: MouseWheelEvent) =
	{
		val baseEvent = super.distributeMouseWheelEvent(event) match
		{
			case Some(consumed) => event.consumed(consumed)
			case None => event
		}
		distributeConsumableEvent(baseEvent) { _.distributeMouseWheelEvent(_) }
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Creates a pop-up over the specified component-area
	  * @param actorHandler Actor handler that will deliver action events for the pop-up
	  * @param over Area over which the pop-up will be displayed
	  * @param alignment Alignment to use when placing the pop-up (default = Right)
	  * @param margin Margin to place between the area and the pop-up (not used with Center alignment)
	  * @param autoCloseLogic Logic used for closing the pop-up (default = won't automatically close the pop-up)
	  * @param makeContent A function for producing pop-up contents based on a component hierarchy
	  * @tparam C Type of created component
	  * @tparam R Type of additional result
	  * @return A component wrapping result that contains the pop-up, the created component inside the canvas and
	  *         the additional result returned by 'makeContent'
	  */
	def createPopup[C <: ReachComponentLike, R](actorHandler: ActorHandler, over: Bounds,
											  alignment: Alignment = Alignment.Right, margin: Double = 0.0,
											  autoCloseLogic: PopupAutoCloseLogic = Never)
											 (makeContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		val newCanvas = ReachCanvas(cursors)(makeContent)
		// FIXME: Normal paint operations don't work while isTransparent = true, but partially transparent windows
		//  don't work when it is false. Avoid this by creating a new pop-up system
		// newCanvas.isTransparent = true
		val popup = Popup(this, newCanvas.parent, actorHandler, autoCloseLogic, alignment) { (_, popupSize) =>
			// Calculates pop-up top left coordinates based on alignment
			Point.calculateWith { axis =>
				alignment.directionAlong(axis) match
				{
					case Some(direction) =>
						direction match
						{
							case Positive => over.maxAlong(axis) + margin
							case Negative => over.minAlong(axis) - popupSize.along(axis) - margin
						}
					case None => over.center.along(axis) - popupSize.along(axis) / 2
				}
			}
		}
		ComponentWrapResult(popup, newCanvas.child, newCanvas.result)
	}
	
	private def distributeConsumableEvent[E <: Consumable[E]](event: E)
	                                                         (action: (ReachCanvas2, E) => Option[ConsumeEvent]) =
		event.distributeAmong(topToBottomCanvases)(action)
	
	
	// NESTED	------------------------------
	
	private class CustomDrawPanel extends JPanel(null)
	{
		// INITIAL CODE	---------------------
		
		setOpaque(true)
		// setBackground(Color.black.toAwt)
		
		// Makes this canvas element focusable and disables the default focus traversal keys
		if (focusEnabled)
		{
			setFocusable(true)
			setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
			setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		}
		
		
		// IMPLEMENTED	----------------------
		
		override def paint(g: Graphics) = paintComponent(g)
		
		override def paintComponent(g: Graphics) = Drawer.use(g) { d =>
			bottomToTopCanvases.foreach { _.currentPainter.foreach { _.paintWith(d) } }
		}
		
		// Never paints children (because won't have any children)
		override def paintChildren(g: Graphics) = ()
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
	
	private class CursorSwapper(cursorManager: ReachCursorManager) extends MouseMoveListener with Handleable
	{
		// ATTRIBUTES	-----------------------------
		
		private var lastMousePosition = Point.origin
		private var lastCursorImage: Option[Image] = None
		
		
		// IMPLEMENTED	-----------------------------
		
		private def averageShadeOf(area: Bounds) =
			topToBottomCanvases.view.filter { _.bounds.contains(area) }.flatMap { _.currentPainter }.headOption match
			{
				case Some(painter) => painter.averageShadeOf(area)
				case None => Dark
			}
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			val newPosition = event.mousePosition - position
			if (lastMousePosition != newPosition)
			{
				lastMousePosition = newPosition
				if (bounds.contains(event.mousePosition))
				{
					val newImage = cursorManager.cursorImageAt(newPosition)(averageShadeOf)
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
*/