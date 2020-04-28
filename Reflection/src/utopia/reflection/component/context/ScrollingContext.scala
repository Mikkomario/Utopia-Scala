package utopia.reflection.component.context

import utopia.genesis.color.Color
import utopia.genesis.shape.LinearAcceleration
import utopia.reflection.component.drawing.immutable.BoxScrollBarDrawer
import utopia.reflection.component.drawing.template.ScrollBarDrawer
import utopia.reflection.container.stack.ScrollAreaLike

object ScrollingContext
{
	// ATTRIBUTES	-----------------------
	
	/**
	  * Scrolling context that draws a back rounded bar inside content with default settings
	  */
	val darkRoundedBarDefault = withRoundedBar()
	
	/**
	  * Scrolling context that draws a light rounded bar inside content with default settings
	  */
	val lightRoundedBarDefault = withRoundedBar(Color.white.withAlpha(0.625))
	
	
	// OTHER	---------------------------
	
	/**
	  * A version of this context that draws a rounded bar inside scrolled content
	  * @param color Bar color (default = 55% alpha black)
	  * @param barWidth Width of the scroll bar in pixels (default = 24)
	  * @param scrollPerWheelClick Amount of pixels scrolled for each wheel "click" (default = 32)
	  * @param scrollFriction Amount of friction applied to scrolling (default = scroll area default (2000px/s2))
	  * @return A new scrolling context
	  */
	def withRoundedBar(color: Color = Color.black.withAlpha(0.55), barWidth: Int = 24,
					   scrollPerWheelClick: Double = 32,
					   scrollFriction: LinearAcceleration = ScrollAreaLike.defaultFriction) =
		ScrollingContext(BoxScrollBarDrawer.roundedBarOnly(color), barWidth, scrollPerWheelClick, scrollFriction,
			scrollBarIsInsideContent = true)
}

/**
  * A component creation context that specifies scrolling settings
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
case class ScrollingContext(scrollBarDrawer: ScrollBarDrawer, scrollBarWidth: Int = 24, scrollPerWheelClick: Double = 32,
							scrollFriction: LinearAcceleration = ScrollAreaLike.defaultFriction,
							scrollBarIsInsideContent: Boolean = false) extends ScrollingContextLike