package utopia.firmament.context
import utopia.firmament.model.Margins
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorScheme
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.firmament.localization.Localizer
import utopia.firmament.model.stack.StackLength
import utopia.flow.util.Mutate

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
	override def withStackMargin(stackMargin: StackLength): Repr = withBase(base.withStackMargin(stackMargin))
	override def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr =
		withBase(base.withAllowImageUpscaling(allowImageUpscaling))
	
	
	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function for the wrapped base context
	  * @return A copy of this context with mapped base context
	  */
	def mapBase(f: Mutate[BaseContext]) = withBase(f(base))
}
