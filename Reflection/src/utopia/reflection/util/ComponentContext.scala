package utopia.reflection.util

import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.LinearAcceleration
import utopia.reflection.component.ComponentLike
import utopia.reflection.component.swing.SwingComponentRelated
import utopia.reflection.container.stack.ScrollBarDrawer
import utopia.reflection.shape.{Alignment, Border, StackInsets, StackLength}
import utopia.reflection.text.Font

/**
  * Used for defining specific settings for multiple components at creation time
  * @author Mikko Hilpinen
  * @since 5.8.2019, v1+
  */
case class ComponentContext(actorHandler: ActorHandler, font: Font, highlightColor: Color, focusColor: Color, normalWidth: Int,
							textColor: Color, promptFont: Font, promptTextColor: Color, textHasMinWidth: Boolean,
							textAlignment: Alignment, background: Option[Color], barBackground: Color, insets: StackInsets,
							border: Option[Border], borderWidth: Double, stackMargin: StackLength,
							relatedItemsStackMargin: StackLength, stackCap: StackLength, dropDownWidthLimit: Option[Int],
							switchWidth: StackLength, textFieldWidth: StackLength, scrollPerWheelClick: Double,
							scrollBarWidth: Int, scrollBarDrawer: ScrollBarDrawer, scrollBarIsInsideContent: Boolean,
							scrollFriction: LinearAcceleration, allowImageUpscaling: Boolean)
{
	// COMPUTED	------------------------
	
	/**
	  * @return Background color used with buttons
	  */
	def buttonBackground = background.getOrElse(highlightColor)
	
	/**
	  * @return Color used for "hint" text, (less important or disabled text)
	  */
	def hintTextColor = textColor.timesAlpha(0.625)
	
	
	// OTHERS	------------------------
	
	/**
	  * Configures component background and/or border if defined
	  * @param component Target component
	  */
	def setBorderAndBackground(component: ComponentLike with SwingComponentRelated) =
	{
		background.foreach { component.background = _ }
		border.foreach(component.setBorder)
	}
}
