package utopia.firmament.context
import utopia.firmament.model.Margins
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorScheme
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.firmament.localization.Localizer
import utopia.reflection.shape.stack.StackLength

/**
  * A common traits for wrappers around the base context
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait BaseContextWrapper[+Repr, +ColorSensitive] extends Any with BaseContextLike[Repr, ColorSensitive]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return The wrapped base context
	  */
	def wrapped: BaseContext
	
	/**
	  * @param baseContext A new base context to wrap
	  * @return A copy of this context with that wrapped data
	  */
	def withBase(baseContext: BaseContext): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def actorHandler = wrapped.actorHandler
	
	override def localizer: Localizer = wrapped.localizer
	
	override def font: Font = wrapped.font
	
	override def colors: ColorScheme = wrapped.colors
	
	override def contrastStandard: ColorContrastStandard = wrapped.contrastStandard
	
	override def stackMargin: StackLength = wrapped.stackMargin
	
	override def smallStackMargin: StackLength = wrapped.smallStackMargin
	
	override def margins = wrapped.margins
	
	override def allowImageUpscaling = wrapped.allowImageUpscaling
	
	override def withFont(font: Font): Repr = withBase(wrapped.withFont(font))
	
	override def withColorContrastStandard(standard: ColorContrastStandard): Repr =
		withBase(wrapped.withColorContrastStandard(standard))
	
	override def withMargins(margins: Margins): Repr = withBase(wrapped.withMargins(margins))
	
	override def withStackMargins(stackMargin: StackLength): Repr = withBase(wrapped.withStackMargins(stackMargin))
	
	override def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr =
		withBase(wrapped.withAllowImageUpscaling(allowImageUpscaling))
}
