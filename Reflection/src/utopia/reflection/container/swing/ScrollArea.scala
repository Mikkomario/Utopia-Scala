package utopia.reflection.container.swing

import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.{Axis2D, LinearAcceleration}
import utopia.genesis.shape.shape2D.Bounds
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.stack.Stackable
import utopia.reflection.component.swing.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.{ScrollAreaLike, ScrollBarDrawer}
import utopia.reflection.shape.StackLengthLimit
import utopia.reflection.util.ComponentContext

import scala.collection.immutable.HashMap

object ScrollArea
{
	/**
	  * Creates a new scroll area using component creation context
	  * @param content Content that will be scrolled
	  * @param lengthLimits Limits applied to scroll area size (default = no limits)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param context Component creation context (implicit)
	  * @tparam C Type of wrapped content
	  * @return A new scroll area
	  */
	def contextual[C <: Stackable with AwtComponentRelated](content: C, lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(),
															limitsToContentSize: Boolean = false)(implicit context: ComponentContext) =
	{
		new ScrollArea[C](content, context.actorHandler, context.scrollPerWheelClick, context.scrollBarDrawer,
			context.scrollBarWidth, context.scrollBarIsInsideContent, context.scrollFriction, lengthLimits, limitsToContentSize)
	}
}

/**
  * This is a 2D scroll area implemented with swing components
  * @author Mikko Hilpinen
  * @since 18.5.2019, v1+
  */
class ScrollArea[C <: Stackable with AwtComponentRelated](override val content: C, actorHandler: ActorHandler,
														  scrollPerWheelClick: Double, scrollBarDrawer: ScrollBarDrawer,
														  override val scrollBarWidth: Int,
														  override val scrollBarIsInsideContent: Boolean = false,
														  override val friction: LinearAcceleration = ScrollAreaLike.defaultFriction,
														  override val lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(),
														  override val limitsToContentSize: Boolean = false)
	extends ScrollAreaLike[C] with AwtComponentWrapperWrapper with CustomDrawableWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	----------------------
	
	private val panel = new Panel[C]()
	
	
	// INITIAL CODE	----------------------
	
	panel += content
	addResizeListener(updateLayout())
	addCustomDrawer(scrollBarDrawerToCustomDrawer(scrollBarDrawer))
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	
	
	// IMPLEMENTED	----------------------
	
	override def children = super[ScrollAreaLike].children
	
	override def axes = Axis2D.values
	
	override def repaint(bounds: Bounds) = panel.component.repaint(bounds.toAwt)
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].isVisible_=(visible)
}
