package utopia.reflection.container.swing.layout.wrapper.scrolling

import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.context.ScrollingContextLike
import utopia.reflection.component.drawing.mutable.MutableCustomDrawableWrapper
import utopia.reflection.component.drawing.template.ScrollBarDrawerLike
import utopia.reflection.component.swing.template.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.stack.template.scrolling.ReflectionScrollViewLike
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.util.ComponentCreationDefaults

object ScrollView
{
	/**
	  * Creates a new scroll view using component creation context
	  * @param content Scroll view contents
	  * @param axis Scroll view axis (default = Y)
	  * @param limitsToContentSize Whether scroll view length should be limited to content length (default = false)
	  * @param context Component creation context (implicit)
	  * @tparam C Type of contained content
	  * @return A new scroll view
	  */
	def contextual[C <: ReflectionStackable with AwtComponentRelated](content: C, axis: Axis2D = Y,
															limitsToContentSize: Boolean = false)
														   (implicit context: ScrollingContextLike) =
	{
		new ScrollView[C](content, axis, context.actorHandler, context.scrollBarDrawer, context.scrollBarWidth,
			context.scrollPerWheelClick, context.scrollFriction, limitsToContentSize,
			context.scrollBarIsInsideContent)
	}
}

/**
  * This is a scroll view implemented with swing components
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
class ScrollView[C <: ReflectionStackable with AwtComponentRelated](override val content: C, override val axis: Axis2D,
                                                                    actorHandler: ActorHandler, scrollBarDrawer: ScrollBarDrawerLike,
                                                                    override val scrollBarWidth: Double = ComponentCreationDefaults.scrollBarWidth,
                                                                    scrollPerWheelClick: Double =  ComponentCreationDefaults.scrollAmountPerWheelClick,
                                                                    override val friction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
                                                                    override val limitsToContentSize: Boolean = false,
                                                                    override val scrollBarIsInsideContent: Boolean = false)
	extends ReflectionScrollViewLike[C] with AwtComponentWrapperWrapper with MutableCustomDrawableWrapper
		with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	----------------------
	
	private val panel = new Panel[C]()
	
	
	// INITIAL CODE	----------------------
	
	panel += content
	addResizeListener(updateLayout())
	addCustomDrawer(scrollBarDrawerToCustomDrawer(scrollBarDrawer))
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	
	
	// IMPLEMENTED	----------------------
	
	override def children = components
	
	// override def repaint(bounds: Bounds) = panel.component.repaint(bounds.toAwt)
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override def scrollBarMargin: Size = Size.zero
	
	override protected def addHierarchyListener[U](listener: Boolean => U): Unit =
		addStackHierarchyChangeListener(listener, callIfAttached = true)
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].visible_=(visible)
}
