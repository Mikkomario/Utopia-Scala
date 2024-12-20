package utopia.firmament.context.base

import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.localization.Localizer
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum

object StaticBaseContext
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * Caches scaled margin pointer-mappings.
	  * The 3 keys come in 2 levels:
	  *     1. Applied variable scaling
	  *     1. Scaled stack margin + applicable margins
	  */
	private val variablyScaledStackMarginCache = WeakCache.weakKeys { scalingP: Changing[SizeCategory] =>
		WeakCache.weakValues[(StackLength, Margins), Changing[StackLength]] { case (margin, margins) =>
			scalingP.map { margins.scaleStackMargin(margin, _) }
		}
	}
	
	
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
	          allowImageUpscaling: Boolean = false)
	         (implicit localizer: Localizer): StaticBaseContext =
		_BaseContext(actorHandler, localizer, font, colorScheme, margins, stackMargins, contrastStandard,
			allowImageUpscaling)
	
	
	// NESTED   --------------------------------------
	
	private case class _BaseContext(actorHandler: ActorHandler, localizer: Localizer, font: Font, colors: ColorScheme,
	                                margins: Margins, customStackMargins: Option[StackLength],
	                                contrastStandard: ColorContrastStandard, allowImageUpscaling: Boolean)
		extends StaticBaseContext
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
		
		override def current: StaticBaseContext = this
		override def toVariableContext: VariableBaseContext =
			VariableBaseContext.fixed(actorHandler, colors, margins, font, contrastStandard, customStackMargins,
				allowImageUpscaling)(localizer)
		
		override def scaledStackMarginPointer(scalingPointer: Changing[SizeCategory]): Changing[StackLength] =
			scalingPointer.fixedValue match {
				case Some(fixedScaling) => scaledStackMarginPointer(fixedScaling)
				case None => variablyScaledStackMarginCache(scalingPointer)(stackMargin -> margins)
			}
		
		override def withFont(font: Font): StaticBaseContext = copy(font = font)
		override def withColorContrastStandard(standard: ColorContrastStandard): StaticBaseContext =
			copy(contrastStandard = standard)
		override def withMargins(margins: Margins): StaticBaseContext = copy(margins = margins)
		override def withStackMargin(stackMargin: StackLength): StaticBaseContext =
			copy(customStackMargins = Some(stackMargin))
		override def withAllowImageUpscaling(allowImageUpscaling: Boolean): StaticBaseContext =
			copy(allowImageUpscaling = allowImageUpscaling)
		
		override def *(mod: Double): StaticBaseContext =
			copy(font = font * mod, margins = margins * mod, customStackMargins = customStackMargins.map { _ * mod })
		
		override def against(background: Color) = StaticColorContext(this, background)
	}
}

/**
  * Common trait for immutable & unchanging base context implementations.
  * Removes generic type parameters from [[StaticBaseContextLike]].
  * @author Mikko Hilpinen
  * @since 29.09.2024, v1.4
  */
trait StaticBaseContext extends BaseContext with StaticBaseContextLike[StaticBaseContext, StaticColorContext]
