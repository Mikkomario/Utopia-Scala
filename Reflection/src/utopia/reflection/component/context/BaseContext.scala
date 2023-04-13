package utopia.reflection.component.context

import utopia.firmament.model.Margins
import utopia.flow.operator.ScopeUsable
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.text.Font
import utopia.reflection.color.{ColorScheme, ComponentColor}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackLength

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
@deprecated("Moved to Firmament", "v2.0")
case class BaseContext(actorHandler: ActorHandler, defaultFont: Font, defaultColorScheme: ColorScheme, margins: Margins,
					   allowImageUpscaling: Boolean = false, stackMarginOverride: Option[StackLength] = None)
	extends BaseContextLike with BackgroundSensitive[ColorContext] with ScopeUsable[BaseContext]
{
	// IMPLEMENTED	------------------------------
	
	override def self = this
	
	override def inContextWithBackground(color: ComponentColor) = ColorContext(this, color)
	
	override def defaultStackMargin = stackMarginOverride.getOrElse(margins.medium.any)
	
	override def relatedItemsStackMargin = stackMarginOverride.map { _ * 0.382 }
		.getOrElse(margins.small.downscaling)
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param stackMargin Stack margin to use
	  * @return A copy of this context with specified stack margin
	  */
	def withStackMargin(stackMargin: StackLength) = copy(stackMarginOverride = Some(stackMargin))
}