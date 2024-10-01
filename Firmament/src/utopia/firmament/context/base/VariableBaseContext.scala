package utopia.firmament.context.base

import utopia.firmament.context.ColorContext
import utopia.firmament.context.color.ColorContext2
import utopia.firmament.localization.Localizer
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{Always, AlwaysFalse, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum

import scala.language.implicitConversions

object VariableBaseContext
{
	// ATTRIBUTES   -------------------------
	
	private val scaledStackMarginCache =
		WeakCache[(Margins, Changing[StackLength], SizeCategory), Changing[StackLength]] { case (margins, from, scaling) =>
			from.map { margins.scaleStackMargin(_, scaling) }
		}
	
	
	// IMPLICIT -----------------------------
	
	/**
	  * Converts a standard base context instance into a variable base context instance
	  * @param context A context instance
	  * @return A variable base context instance
	  */
	implicit def from(context: BaseContext2): VariableBaseContext = context match {
		case v: VariableBaseContext => v
		case c =>
			apply(c.actorHandler, c.margins, c.colors, c.fontPointer, c.contrastStandard, Some(c.stackMarginPointer),
				c.allowImageUpscalingFlag)(c.localizer)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new variable base context
	  * @param actorHandler Actor handler used for generating / delivering the action events
	  * @param fontPointer Pointer that contains the applied font
	  * @param margins Applied margins
	  * @param colors Applied color-scheme
	  * @param contrastStandard The color contrast standard applied (default = always minimum standard)
	  * @param stackMarginPointer Pointer that contains the applied default stack margin.
	  *                           None (default) if the default pointer should be generated & used instead.
	  * @param allowImageUpscalingFlag Flag that determines whether image-upscaling should be allowed.
	  *                                Default = never allowed.
	  * @param localizer Implicit localization implementation
	  * @return A new variable context instance
	  */
	def apply(actorHandler: ActorHandler, margins: Margins, colors: ColorScheme,
	          fontPointer: Changing[Font], contrastStandard: ColorContrastStandard = Minimum,
	          stackMarginPointer: Option[Changing[StackLength]] = None,
	          allowImageUpscalingFlag: Flag = AlwaysFalse)
	         (implicit localizer: Localizer): VariableBaseContext =
		_VariableBaseContext(actorHandler, localizer, margins, colors, contrastStandard, fontPointer,
			stackMarginPointer.getOrElse { Fixed(defaultStackMarginWith(margins)) },
			Lazy { createSmallStackMarginPointer(margins, stackMarginPointer) }, allowImageUpscalingFlag,
			stackMarginPointerIsCustom = false)
	/**
	  * Creates a new static base context
	  * @param actorHandler Actor handler used for generating / delivering the action events
	  * @param font The applied font
	  * @param colorScheme The applied color-scheme
	  * @param margins The applied margins
	  * @param stackMargin The applied default stack margin.
	  *                    None (default) if the default margin should be generated & used instead.
	  * @param colorContrastStandard The color contrast standard applied
	  *                              (default = minimum standard)
	  * @param allowImageUpscaling Whether image-upscaling should be allowed. Default = false = no.
	  * @param localizer Implicit localization implementation
	  * @return A new static context instance
	  */
	def fixed(actorHandler: ActorHandler, colorScheme: ColorScheme, margins: Margins, font: Font,
	          colorContrastStandard: ColorContrastStandard = Minimum, stackMargin: Option[StackLength] = None,
	          allowImageUpscaling: Boolean = false)
	         (implicit localizer: Localizer): VariableBaseContext =
		apply(actorHandler, margins, colorScheme, Fixed(font), colorContrastStandard, stackMargin.map { Fixed(_) },
			Always(allowImageUpscaling))
	
	private def createColorPointersCache(colorSchemePointer: Changing[ColorScheme]) =
		WeakCache[(ColorRole, ColorLevel), Changing[Color]] { case (role, shade) =>
			colorSchemePointer.map { _(role)(shade) }
		}
	
	private def defaultStackMarginWith(margins: Margins) = StackLength(margins.verySmall, margins.medium, margins.large)
	private def createSmallStackMarginPointer(margins: Margins,
	                                          customStackMarginPointer: Option[Changing[StackLength]] = None) =
		customStackMarginPointer match {
			case Some(stackMarginPointer) => stackMarginPointer.map { _ * margins.adjustment(-1) }
			case None => Fixed(StackLength(0, margins.small, margins.medium))
		}
	
	
	// NESTED   -----------------------------
	
	private case class _VariableBaseContext(actorHandler: ActorHandler, localizer: Localizer,
	                                        margins: Margins, colors: ColorScheme,
	                                        contrastStandard: ColorContrastStandard,
	                                        fontPointer: Changing[Font],
	                                        stackMarginPointer: Changing[StackLength],
	                                        smallStackMarginPointerView: View[Changing[StackLength]],
	                                        allowImageUpscalingFlag: Flag,
	                                        stackMarginPointerIsCustom: Boolean)
		extends VariableBaseContext
	{
		// IMPLEMENTED  ------------------------
		
		override def self: VariableBaseContext = this
		
		override def smallStackMarginPointer: Changing[StackLength] = smallStackMarginPointerView.value
		
		override def withColorContrastStandard(standard: ColorContrastStandard): VariableBaseContext =
			copy(contrastStandard = standard)
		override def withMargins(margins: Margins): VariableBaseContext = copy(margins = margins)
		
		override def withFontPointer(p: Changing[Font]): VariableBaseContext = copy(fontPointer = p)
		override def withStackMarginPointer(p: Changing[StackLength]): VariableBaseContext =
			copy(stackMarginPointer = p,
				smallStackMarginPointerView = Lazy { createSmallStackMarginPointer(margins, Some(p)) },
				stackMarginPointerIsCustom = true)
		override def withAllowImageUpscalingFlag(f: Flag): VariableBaseContext = copy(allowImageUpscalingFlag = f)
		
		// FIXME: Implement
		override def against(backgroundPointer: Changing[Color]) = ???
		
		override def *(mod: Double): VariableBaseContext = {
			if (mod == 1)
				this
			else {
				val newFontPointer = fontPointer.map { _ * mod }
				val newMargins = margins * mod
				val newStackMarginPointer = {
					if (stackMarginPointerIsCustom)
						stackMarginPointer.map { _ * mod }
					else
						Fixed(defaultStackMarginWith(newMargins))
				}
				val newSmallStackMarginPointerView = Lazy {
					createSmallStackMarginPointer(newMargins,
						Some(newStackMarginPointer).filter { _ => stackMarginPointerIsCustom })
				}
				
				copy(fontPointer = newFontPointer, margins = newMargins, stackMarginPointer = newStackMarginPointer,
					smallStackMarginPointerView = newSmallStackMarginPointerView)
			}
		}
		
		override def scaledStackMarginPointer(scaling: SizeCategory): Changing[StackLength] =
			scaledStackMarginCache((margins, stackMarginPointer, scaling))
	}
}

/**
  * Common trait for pointer-based contexts which are generally applicable
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.3.1
  */
// TODO: Change second param to VariableColorContext
trait VariableBaseContext extends BaseContext2 with VariableBaseContextLike[VariableBaseContext, ColorContext2]