package utopia.reflection.container.swing.layout.wrapper.scrolling

import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.context.ScrollingContextLike
import utopia.reflection.component.drawing.mutable.MutableCustomDrawableWrapper
import utopia.reflection.component.drawing.template.ScrollBarDrawerLike
import utopia.reflection.component.swing.template.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.stack.template.scrolling.ReflectionScrollAreaLike
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.util.ComponentCreationDefaults

object ScrollArea
{
	/**
	  * Creates a new scroll area using component creation context
	  * @param content Content that will be scrolled
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param context Component creation context (implicit)
	  * @tparam C Type of wrapped content
	  * @return A new scroll area
	  */
	def contextual[C <: ReflectionStackable with AwtComponentRelated](content: C, limitsToContentSize: Boolean = false)
	                                                                 (implicit context: ScrollingContextLike) =
	{
		new ScrollArea[C](content, context.actorHandler, context.scrollBarDrawer, context.scrollBarWidth,
			context.scrollPerWheelClick, context.scrollFriction, limitsToContentSize,
			context.scrollBarIsInsideContent)
	}
}

/**
  * This is a 2D scroll area implemented with swing components
  * @author Mikko Hilpinen
  * @since 18.5.2019, v1+
  */
class ScrollArea[C <: ReflectionStackable with AwtComponentRelated](override val content: C, actorHandler: ActorHandler,
                                                          scrollBarDrawer: ScrollBarDrawerLike,
                                                          override val scrollBarWidth: Double = ComponentCreationDefaults.scrollBarWidth,
                                                          scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
                                                          override val friction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
                                                          override val limitsToContentSize: Boolean = false,
                                                          override val scrollBarIsInsideContent: Boolean = false)
	extends ReflectionScrollAreaLike[C] with AwtComponentWrapperWrapper with MutableCustomDrawableWrapper with AwtContainerRelated
		with SwingComponentRelated
{
	// ATTRIBUTES	----------------------
	
	private val panel = new Panel[C]()
	
	
	// INITIAL CODE	----------------------
	
	panel += content
	addResizeListener(updateLayout())
	addCustomDrawer(scrollBarDrawerToCustomDrawer(scrollBarDrawer))
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	
	
	// IMPLEMENTED	----------------------
	
	override def scrollBarMargin: Size = Size.zero
	
	override def children = components
	
	override def axes = Axis2D.values
	
	// override def repaint(bounds: Bounds) = panel.component.repaint(bounds.toAwt)
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override protected def addHierarchyListener[U](listener: Boolean => U): Unit =
		addStackHierarchyChangeListener(listener, callIfAttached = true)
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].visible_=(visible)
}
