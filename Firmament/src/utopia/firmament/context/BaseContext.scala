package utopia.firmament.context

import utopia.firmament.localization.Localizer
import utopia.firmament.model.Margins
import utopia.firmament.model.stack.StackLength
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum

object BaseContext
{
	// OTHER    -------------------------
	
	/**
	  * @param actorHandler Actor handler used
	  * @param font Font to use
	  * @param colorScheme Color scheme to use
	  * @param margins Margins to use
	  * @param stackMargins Stack margins to use (default = use margins)
	  * @param contrastStandard Color contrast standards to apply (default = minimum)
	  * @param allowImageUpscaling Whether image upscaling should be allowed (default = false)
	  * @param localizer Localization implementation to use (implicit)
	  * @return A new base context
	  */
	def apply(actorHandler: ActorHandler, font: Font, colorScheme: ColorScheme, margins: Margins,
	          stackMargins: Option[StackLength] = None, contrastStandard: ColorContrastStandard = Minimum,
	          allowImageUpscaling: Boolean = false)(implicit localizer: Localizer): BaseContext =
		_BaseContext(actorHandler, localizer, font, colorScheme, margins, stackMargins, contrastStandard,
			allowImageUpscaling)
	
	
	// NESTED   --------------------------------------
	
	private case class _BaseContext(actorHandler: ActorHandler, localizer: Localizer, font: Font, colors: ColorScheme,
	                                margins: Margins, customStackMargins: Option[StackLength],
	                                contrastStandard: ColorContrastStandard, allowImageUpscaling: Boolean)
		extends BaseContext
	{
		// ATTRIBUTES   ------------------------------
		
		override lazy val stackMargin: StackLength = customStackMargins
			.getOrElse(StackLength(margins.verySmall, margins.medium, margins.large))
		override lazy val smallStackMargin = customStackMargins match {
			case Some(margins) => margins * this.margins.adjustment(-1)
			case None => StackLength(0, margins.small, margins.medium)
		}
		
		
		// IMPLEMENTED	------------------------------
		
		override def self = this
		
		override def withFont(font: Font): BaseContext = copy(font = font)
		override def withColorContrastStandard(standard: ColorContrastStandard): BaseContext =
			copy(contrastStandard = standard)
		override def withMargins(margins: Margins): BaseContext = copy(margins = margins)
		override def withStackMargins(stackMargin: StackLength): BaseContext =
			copy(customStackMargins = Some(stackMargin))
		override def withAllowImageUpscaling(allowImageUpscaling: Boolean): BaseContext =
			copy(allowImageUpscaling = allowImageUpscaling)
		
		override def *(mod: Double): BaseContext =
			copy(font = font * mod, margins = margins * mod, customStackMargins = customStackMargins.map { _ * mod })
		
		override def against(background: Color): ColorContext = ColorContext(this, background)
	}
}

/**
  * This component context specifies information that is shared within the whole program (not component specific)
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait BaseContext extends BaseContextLike[BaseContext, ColorContext]