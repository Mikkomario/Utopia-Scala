package utopia.reflection.component.context

import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.reflection.component.drawing.template.ScrollBarDrawerLike

/**
  * A common trait for contexts that define scrolling-related settings
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
@deprecated("Moved to Firmament", "v2.0")
trait ScrollingContextLike
{
	/**
	  * @return Actor handler used for delivering scrolling-related action events
	  */
	def actorHandler: ActorHandler
	
	/**
	  * @return The amount of pixels scrolled for each "click" of the mouse wheel
	  */
	def scrollPerWheelClick: Double
	
	/**
	  * @return Friction applied to continuous scrolling
	  */
	def scrollFriction: LinearAcceleration
	
	/**
	  * @return The width of a scroll bar in pixels
	  */
	def scrollBarWidth: Int
	
	/**
	  * @return Whether scroll bar should be drawn inside (true) or outside (false) the scrolled content
	  */
	def scrollBarIsInsideContent: Boolean
	
	/**
	  * @return Drawer that is used for drawing the scroll bar
	  */
	def scrollBarDrawer: ScrollBarDrawerLike
}
