package utopia.reflection.container.swing

import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.{Axis2D, LinearAcceleration}
import utopia.genesis.shape.shape2D.Bounds
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.stack.Stackable
import utopia.reflection.component.swing.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.{ScrollAreaLike, ScrollBarDrawer, ScrollViewLike, StackHierarchyManager}
import utopia.reflection.shape.StackLengthLimit
import utopia.reflection.util.ComponentContext

object ScrollView
{
	/**
	  * Creates a new scroll view using component creation context
	  * @param content Scroll view contents
	  * @param axis Scroll view axis (default = Y)
	  * @param lengthLimits Scroll view length limits (default = no limits)
	  * @param limitsToContentSize Whether scroll view length should be limited to content length (default = false)
	  * @param context Component creation context (implicit)
	  * @tparam C Type of contained content
	  * @return A new scroll view
	  */
	def contextual[C <: Stackable with AwtComponentRelated](content: C, axis: Axis2D = Y,
															lengthLimits: StackLengthLimit = StackLengthLimit.noLimit,
															limitsToContentSize: Boolean = false)
														   (implicit context: ComponentContext) =
	{
		new ScrollView[C](content, axis, context.actorHandler, context.scrollPerWheelClick, context.scrollBarDrawer,
			context.scrollBarWidth, context.scrollBarIsInsideContent, context.scrollFriction, lengthLimits, limitsToContentSize)
	}
}

/**
  * This is a scroll view implemented with swing components
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
class ScrollView[C <: Stackable with AwtComponentRelated](override val content: C, override val axis: Axis2D,
														  actorHandler: ActorHandler, scrollPerWheelClick: Double,
														  scrollBarDrawer: ScrollBarDrawer, override val scrollBarWidth: Int,
														  override val scrollBarIsInsideContent: Boolean = false,
														  override val friction: LinearAcceleration = ScrollAreaLike.defaultFriction,
														  override val lengthLimit: StackLengthLimit = StackLengthLimit.noLimit,
														  override val limitsToContentSize: Boolean = false)
	extends ScrollViewLike[C] with AwtComponentWrapperWrapper with CustomDrawableWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	----------------------
	
	private val panel = new Panel[C]()
	
	
	// INITIAL CODE	----------------------
	
	panel += content
	addResizeListener(updateLayout())
	addCustomDrawer(scrollBarDrawerToCustomDrawer(scrollBarDrawer))
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	
	
	// IMPLEMENTED	----------------------
	
	override def children = super[ScrollViewLike].children
	
	override def repaint(bounds: Bounds) = panel.component.repaint(bounds.toAwt)
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].isVisible_=(visible)
}
