package utopia.firmament.context.base

import utopia.firmament.localization.Localizer
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.util.Mutate
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorScheme
import utopia.paradigm.enumeration.ColorContrastStandard

/**
  * Common trait for implementations of BaseContext by wrapping another instance
  * @tparam Base Type of the wrapped base context
  * @tparam Repr Type of this context
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.4
  */
trait BaseContextWrapper2[Base <: BaseContextCopyable[Base, _], +Repr] extends BaseContextCopyable[Repr, Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The wrapped base context instance
	  */
	def base: Base
	/**
	  * @param base New base context instance to wrap
	  * @return Copy of this wrapper with the specified based context
	  */
	def withBase(base: Base): Repr
	
	
	// IMPLEMENTED  ---------------------
	
	override def withFont(font: Font): Repr = mapBase { _.withFont(font) }
	override def withColorContrastStandard(standard: ColorContrastStandard): Repr =
		mapBase { _.withColorContrastStandard(standard) }
	override def withMargins(margins: Margins): Repr = mapBase { _.withMargins(margins) }
	override def withStackMargin(stackMargin: StackLength): Repr = mapBase { _.withStackMargin(stackMargin) }
	override def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr =
		mapBase { _.withAllowImageUpscaling(allowImageUpscaling) }
	override def withStackMargin(size: SizeCategory): Repr = mapBase { _.withStackMargin(size) }
	
	override def mapFont(f: Font => Font): Repr = mapBase { _.mapFont(f) }
	override def mapStackMargin(f: StackLength => StackLength): Repr = mapBase { _.mapStackMargin(f) }
	
	override def actorHandler: ActorHandler = base.actorHandler
	override def localizer: Localizer = base.localizer
	override def colors: ColorScheme = base.colors
	override def contrastStandard: ColorContrastStandard = base.contrastStandard
	override def margins: Margins = base.margins
	
	override def fontPointer: Changing[Font] = base.fontPointer
	override def stackMarginPointer: Changing[StackLength] = base.stackMarginPointer
	override def smallStackMarginPointer: Changing[StackLength] = base.smallStackMarginPointer
	override def allowImageUpscalingFlag: Flag = base.allowImageUpscalingFlag
	
	override def scaledStackMarginPointer(scaling: SizeCategory): Changing[StackLength] =
		base.scaledStackMarginPointer(scaling)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function applied to the wrapped base context
	  * @return Copy of this context with the modified base context
	  */
	def mapBase(f: Mutate[Base]): Repr = withBase(f(base))
}
