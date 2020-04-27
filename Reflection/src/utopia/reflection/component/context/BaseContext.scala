package utopia.reflection.component.context

import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.color.{ColorScheme, ComponentColor}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font

/**
  * This component context specifies information that is shared within the whole program (not component specific)
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  * @param actorHandler Actor handler used for distributing action events for the components
  * @param defaultFont The font used as a basis / by default
  * @param defaultColorScheme Color scheme used by default
  * @param margins Sizes of various types of margins
  */
case class BaseContext(actorHandler: ActorHandler, defaultFont: Font, defaultColorScheme: ColorScheme, margins: Margins)
	extends BaseContextLike
{
	/**
	  * @param containerBackground Background color of the containing container
	  * @return A new copy of this context with background color information
	  */
	def inContainerWithBackground(containerBackground: ComponentColor) = ColorContext(this, containerBackground)
}