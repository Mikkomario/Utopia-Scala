package utopia.firmament.context
import utopia.paradigm.color.{Color, ColorSet}

/**
  * A common trait for contexts that wrap the color context
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
@deprecated("Replaced with a new version", "v1.4")
trait ColorContextWrapper[+Repr, +Textual] extends ColorContextLike[Repr, Textual]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return The color context wrapped by this context
	  */
	def colorContext: ColorContext
	
	/**
	  * @param base New color context to wrap
	  * @return A copy of this context, wrapping the specified color context
	  */
	def withColorContext(base: ColorContext): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def base: BaseContext = colorContext
	
	override def background: Color = colorContext.background
	override def textColor: Color = colorContext.textColor
	
	override def withDefaultTextColor: Repr = withColorContext(colorContext.withDefaultTextColor)
	override def withTextColor(color: Color): Repr = withColorContext(colorContext.withTextColor(color))
	override def withTextColor(color: ColorSet): Repr = withColorContext(colorContext.withTextColor(color))
	
	override def against(background: Color): Repr = withColorContext(colorContext.against(background))
	
	override def withBase(baseContext: BaseContext): Repr = withColorContext(colorContext.withBase(baseContext))
	
	
	// OTHER    ----------------------
	
	/**
	  * @param f A mapping function for the wrapped color context
	  * @return A copy of this context with mapped color context
	  */
	def mapColorContext(f: ColorContext => ColorContext) = withColorContext(f(colorContext))
}
