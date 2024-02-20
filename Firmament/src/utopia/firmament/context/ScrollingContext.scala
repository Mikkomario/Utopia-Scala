package utopia.firmament.context

import utopia.firmament.drawing.immutable.BoxScrollBarDrawer
import utopia.firmament.drawing.template.ScrollBarDrawerLike
import utopia.genesis.handling.action.ActorHandler2
import utopia.paradigm.color.Color
import utopia.paradigm.motion.motion1d.LinearAcceleration

object ScrollingContext
{
	// OTHER	---------------------------
	
	/**
	  * @param actorHandler Actor handler to use in distributing scrolling-related action events
	  * @param scrollBarDrawer Drawer used for visualizing the scroll bar
	  * @param scrollBarWidth Scroll bar width (default = common default)
	  * @param scrollPerWheelClick Amount of pixels scrolled on each mouse wheel "click" (default = common default)
	  * @param scrollFriction Scroll friction to apply (default = common default)
	  * @param scrollBarIsInsideContent Whether the scroll bar should be drawn over the scroll area contents
	  *                                 (default = false = draw the scroll bar over a separate area)
	  * @return A new scrolling context instance
	  */
	def apply(actorHandler: ActorHandler2, scrollBarDrawer: ScrollBarDrawerLike,
	          scrollBarWidth: Int = ComponentCreationDefaults.scrollBarWidth,
	          scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
	          scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
	          scrollBarIsInsideContent: Boolean = false): ScrollingContext =
		_ScrollingContext(actorHandler, scrollBarDrawer, scrollBarWidth, scrollPerWheelClick, scrollFriction,
			scrollBarIsInsideContent)
	
	/**
	  * A version of this context that draws a rounded bar inside scrolled content
	  * @param actorHandler        Actor handler used for delivering action events
	  * @param color               Bar color (default = 55% alpha black)
	  * @param barWidth            Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction      Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return A new scrolling context
	  */
	def withRoundedBar(actorHandler: ActorHandler2, color: Color = Color.black.withAlpha(0.55),
	                   barWidth: Int = ComponentCreationDefaults.scrollBarWidth,
	                   scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
	                   scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction) =
		ScrollingContext(actorHandler, BoxScrollBarDrawer.roundedBarOnly(color), barWidth, scrollPerWheelClick,
			scrollFriction, scrollBarIsInsideContent = true)
	
	/**
	  * @param actorHandler        Actor handler used for delivering action events
	  * @param barWidth            Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction      Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return Scrolling context that draws a dark rounded bar inside content
	  */
	def withDarkRoundedBar(actorHandler: ActorHandler2, barWidth: Int = ComponentCreationDefaults.scrollBarWidth,
	                       scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
	                       scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction) =
		withRoundedBar(actorHandler, barWidth = barWidth, scrollPerWheelClick = scrollPerWheelClick, scrollFriction = scrollFriction)
	/**
	  * @param actorHandler        Actor handler used for delivering action events
	  * @param barWidth            Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction      Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return Scrolling context that draws a light rounded bar inside content
	  */
	def withLightRoundedBar(actorHandler: ActorHandler2, barWidth: Int = ComponentCreationDefaults.scrollBarWidth,
	                        scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
	                        scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction) =
		withRoundedBar(actorHandler, Color.white.withAlpha(0.625), barWidth, scrollPerWheelClick, scrollFriction)
	
	
	// NESTED   ---------------------------
	
	case class _ScrollingContext(actorHandler: ActorHandler2, scrollBarDrawer: ScrollBarDrawerLike, scrollBarWidth: Int,
	                             scrollPerWheelClick: Double, scrollFriction: LinearAcceleration,
	                             scrollBarIsInsideContent: Boolean)
		extends ScrollingContext
}

/**
  * A common trait for contexts that define scrolling-related settings
  * @author Mikko Hilpinen
  * @since 28.4.2020, Reflection v1.2
  */
trait ScrollingContext
{
	/**
	  * @return Actor handler used for delivering scrolling-related action events
	  */
	def actorHandler: ActorHandler2
	
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
