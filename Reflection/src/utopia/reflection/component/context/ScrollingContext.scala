package utopia.reflection.component.context

import utopia.paradigm.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.reflection.component.drawing.immutable.BoxScrollBarDrawer
import utopia.reflection.component.drawing.template.ScrollBarDrawerLike
import utopia.reflection.util.ComponentCreationDefaults

object ScrollingContext
{
	// OTHER	---------------------------
	
	/**
	  * A version of this context that draws a rounded bar inside scrolled content
	  * @param actorHandler Actor handler used for delivering action events
	  * @param color Bar color (default = 55% alpha black)
	  * @param barWidth Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return A new scrolling context
	  */
	def withRoundedBar(actorHandler: ActorHandler, color: Color = Color.black.withAlpha(0.55),
					   barWidth: Int = ComponentCreationDefaults.scrollBarWidth,
					   scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
					   scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction) =
		ScrollingContext(actorHandler, BoxScrollBarDrawer.roundedBarOnly(color), barWidth, scrollPerWheelClick, scrollFriction,
			scrollBarIsInsideContent = true)
	
	/**
	  * @param actorHandler Actor handler used for delivering action events
	  * @param barWidth Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return Scrolling context that draws a dark rounded bar inside content
	  */
	def withDarkRoundedBar(actorHandler: ActorHandler, barWidth: Int = ComponentCreationDefaults.scrollBarWidth,
						   scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
						   scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction) =
		withRoundedBar(actorHandler, barWidth = barWidth, scrollPerWheelClick = scrollPerWheelClick, scrollFriction = scrollFriction)
	
	/**
	  * @param actorHandler Actor handler used for delivering action events
	  * @param barWidth Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return Scrolling context that draws a light rounded bar inside content
	  */
	def withLightRoundedBar(actorHandler: ActorHandler, barWidth: Int = ComponentCreationDefaults.scrollBarWidth,
							scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
							scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction) =
		withRoundedBar(actorHandler, Color.white.withAlpha(0.625), barWidth, scrollPerWheelClick, scrollFriction)
}

/**
  * A component creation context that specifies scrolling settings
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
case class ScrollingContext(actorHandler: ActorHandler, scrollBarDrawer: ScrollBarDrawerLike,
							scrollBarWidth: Int = ComponentCreationDefaults.scrollBarWidth,
							scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
							scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
							scrollBarIsInsideContent: Boolean = false) extends ScrollingContextLike