package utopia.firmament.context
import utopia.firmament.model.Margins
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorScheme
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.firmament.localization.Localizer
import utopia.firmament.model.stack.StackLength

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
	def base: BaseContext
	
	/**
	  * @param baseContext A new base context to wrap
	  * @return A copy of this context with that wrapped data
	  */
	def withBase(baseContext: BaseContext): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override implicit def localizer: Localizer = base.localizer
	
	override def actorHandler = base.actorHandler
	override def font: Font = base.font
	override def colors: ColorScheme = base.colors
	override def contrastStandard: ColorContrastStandard = base.contrastStandard
	override def stackMargin: StackLength = base.stackMargin
	override def smallStackMargin: StackLength = base.smallStackMargin
	override def margins = base.margins
	override def allowImageUpscaling = base.allowImageUpscaling
	
	override def withFont(font: Font): Repr = withBase(base.withFont(font))
	override def withColorContrastStandard(standard: ColorContrastStandard): Repr =
		withBase(base.withColorContrastStandard(standard))
	override def withMargins(margins: Margins): Repr = withBase(base.withMargins(margins))
	override def withStackMargins(stackMargin: StackLength): Repr = withBase(base.withStackMargins(stackMargin))
	override def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr =
		withBase(base.withAllowImageUpscaling(allowImageUpscaling))
	
	
	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function for the wrapped base context
	  * @return A copy of this context with mapped base context
	  */
	def mapBase(f: BaseContext => BaseContext) = withBase(f(base))
}
