package utopia.reflection.component.context

import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.color.{ColorScheme, ComponentColor}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.StackLength

/**
  * This component context specifies information that is shared within the whole program (not component specific)
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  * @param actorHandler Actor handler used for distributing action events for the components
  * @param defaultFont The font used as a basis / by default
  * @param defaultColorScheme Color scheme used by default
  * @param margins Sizes of various types of margins
  * @param allowImageUpscaling Whether images should be allowed to scale above their original resolution (default = false)
  */
case class BaseContext(actorHandler: ActorHandler, defaultFont: Font, defaultColorScheme: ColorScheme, margins: Margins,
					   allowImageUpscaling: Boolean = false, stackMarginOverride: Option[StackLength] = None)
	extends BaseContextLike with BackgroundSensitive[ColorContext] with ScopeUsable[BaseContext]
{
	// IMPLEMENTED	------------------------------
	
	override def repr = this
	
	override def inContextWithBackground(color: ComponentColor) = ColorContext(this, color)
	
	override def defaultStackMargin = stackMarginOverride.getOrElse(margins.medium.any)
	
	override def relatedItemsStackMargin = stackMarginOverride.map { _ / 2 }.getOrElse(margins.small.downscaling)
}