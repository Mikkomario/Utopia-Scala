package utopia.reflection.reach

import utopia.firmament.context.ComponentCreationDefaults
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.util.NotEmpty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag, SettableOnce}
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.ReachCanvas
import utopia.reach.cursor.CursorSet
import utopia.reflection.component.swing.template.{AwtComponentRelated, AwtComponentWrapper, SwingComponentRelated}
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.event.{ResizeEvent, ResizeListener, StackHierarchyListener}
import utopia.reflection.reach.ReflectionReachCanvas.ParentWrapper

import scala.concurrent.ExecutionContext

object ReflectionReachCanvas
{
	// OTHER    -----------------------------
	
	/**
	  * Creates a new Reach canvas
	  * @param background                 The background color to use in this canvas, initially.
	  *                                   Use color with alpha=0.0 to display this canvas as transparent.
	  * @param cursors                    A set of custom cursors to use on this canvas (optional)
	  * @param enableAwtDoubleBuffering   Whether AWT double buffering should be allowed.
	  *                                   Setting this to true might make the painting less responsive.
	  *                                   Default = false = disable AWT double buffering.
	  * @param disableFocus               Whether this canvas shall not be allowed to gain or manage focus.
	  *                                   Default = false = focus is enabled.
	  * @param createContent              A function that accepts the created canvas' component hierarchy and yields the main
	  *                                   content component that this canvas will wrap.
	  *                                   May return an additional result, which will be returned by this function also.
	  * @param exc                        Implicit execution context
	  * @param log                        Implicit logging implementation for some error cases
	  * @tparam C Type of the component wrapped by this canvas
	  * @tparam R Type of the additional result from the 'createContent' function
	  * @return The created canvas + the created content component + the additional result returned by 'createContent'
	  */
	def apply[C <: ReachComponentLike, R](background: Color, cursors: Option[CursorSet] = None,
	                                      enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false)
	                                     (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                     (implicit exc: ExecutionContext, log: Logger) =
	{
		// Creates the canvas
		val canvasPointer = SettableOnce[ReflectionReachCanvas]()
		lazy val absolutePositionView: View[Point] = View {
			canvasPointer.value.flatMap { _.parent } match {
				case Some(parent) => parent.absolutePosition
				case None => Point.origin
			}
		}
		val contentPointer = SettableOnce[ReachComponentLike]()
		val canvas = new ReflectionReachCanvas(contentPointer, absolutePositionView,
			EventfulPointer[Color](background), ResettableFlag(), cursors, enableAwtDoubleBuffering,
			disableFocus)({
			case c: ReflectionStackable => c.revalidate()
			case _ => canvasPointer.value.foreach { _.revalidate() }
		})
		canvasPointer.set(canvas)
		
		// Then creates the content, using the canvas' component hierarchy
		val newContent = createContent(canvas.hierarchy)
		contentPointer.set(newContent.component)
		
		newContent in canvas
	}
	
	
	// NESTED   --------------------------
	
	private class ParentWrapper(override val component: java.awt.Component,
	                            child: ReflectionComponentLike with AwtComponentRelated)
		extends AwtComponentWrapper
	{
		override val children = Single(child)
	}
}

/**
  * A ReachCanvas implementation that may be used in Utopia Reflection
  *
  * @constructor Creates a new Reach canvas.
  *              This constructor is protected and only intended for potential inheritance.
  *              Please use ReflectionReachCanvas.apply(...) instead.
  * @param contentPointer Pointer that will contain the content to assign to this canvas.
  *                       None initially. Not expected to change once initialized.
  * @param backgroundPointer A mutable pointer that contains the background color used in this component.
  *                          Use a fully transparent background (i.e. alpha=0.0)
  *                          if you want this component to be transparent.
  * @param attachmentPointer A mutable pointer that contains true when this canvas is attached to the
  *                          main stack hierarchy (managed from this component).
  *                          Default = new pointer.
  * @param cursors                  A set of custom cursors to use on this canvas (optional)
  * @param enableAwtDoubleBuffering Whether AWT double buffering should be allowed.
  *                                 Setting this to true might make the painting less responsive.
  *                                 Default = false = disable AWT double buffering.
  * @param disableFocus             Whether this canvas shall not be allowed to gain or manage focus.
  *                                 Default = false = focus is enabled.
  * @param revalidateImplementation Implementation for the revalidate() function.
  *                                 Accepts this ReachCanvas instance.
  *                                 Should call the revalidate() in this canvas (from ReflectionStackable)
  *                                 or implement similar functionality.
  *
  * @author Mikko Hilpinen
  * @since 19.4.2023, v1.0
  */
class ReflectionReachCanvas protected(contentPointer: Changing[Option[ReachComponentLike]],
                                      absoluteParentPositionView: => View[Point],
                                      backgroundPointer: EventfulPointer[Color],
                                      attachmentPointer: ResettableFlag = ResettableFlag()(ComponentCreationDefaults.componentLogger),
                                      cursors: Option[CursorSet] = None,
                                      enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false)
                                     (revalidateImplementation: ReachCanvas => Unit)
                                     (implicit exc: ExecutionContext)
	extends ReachCanvas(contentPointer, attachmentPointer, Left(absoluteParentPositionView), backgroundPointer, cursors,
		enableAwtDoubleBuffering, disableFocus)(revalidateImplementation)
		with ReflectionStackable with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES   ----------------------
	
	override val stackId: Int = hashCode()
	
	private var _visible = true
	
	private var _stackHierarchyListeners: Seq[StackHierarchyListener] = Empty
	private var _resizeListeners: Seq[ResizeListener] = Empty
	
	override lazy val parent: Option[ReflectionComponentLike] =
		Option(component.getParent).map { new ParentWrapper(_, this) }
	
	
	// INITIAL CODE ----------------------
	
	// Informs resize listeners on size changes
	sizePointer.addContinuousListener { e =>
		NotEmpty(resizeListeners).foreach { listeners =>
			val resizeEvent = ResizeEvent(e.oldValue, e.newValue)
			listeners.foreach { _.onResizeEvent(resizeEvent) }
		}
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def bounds = super[ReachCanvas].bounds
	override def bounds_=(b: Bounds) = super[ReachCanvas].bounds_=(b)
	
	override def resizeListeners: Seq[ResizeListener] = _resizeListeners
	override def resizeListeners_=(listeners: Seq[ResizeListener]): Unit = _resizeListeners = listeners
	
	override def visible: Boolean = _visible
	override def visible_=(isVisible: Boolean): Unit = {
		if (_visible != isVisible) {
			_visible = isVisible
			revalidate()
		}
	}
	
	override def background: Color = backgroundPointer.value
	override def background_=(color: Color): Unit = backgroundPointer.value = color
	
	override def isTransparent: Boolean = backgroundPointer.value.alpha < 1.0
	
	override def parentWindow = super[ReachCanvas].parentWindow
	override def absolutePosition = super[ReflectionStackable].absolutePosition
	
	override def isAttachedToMainHierarchy: Boolean = attachmentPointer.value
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean): Unit =
		attachmentPointer.value = newAttachmentStatus
	
	override def stackHierarchyListeners: Seq[StackHierarchyListener] = _stackHierarchyListeners
	override def stackHierarchyListeners_=(newListeners: Seq[StackHierarchyListener]): Unit =
		_stackHierarchyListeners = newListeners
	
	override def revalidate() = super[ReflectionStackable].revalidate()
}
