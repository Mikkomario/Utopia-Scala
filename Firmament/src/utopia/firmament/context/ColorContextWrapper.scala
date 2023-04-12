package utopia.firmament.context
import utopia.paradigm.color.{Color, ColorSet}

/**
  * A common trait for contexts that wrap the color context
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait ColorContextWrapper[+Repr, +Textual] extends ColorContextLike[Repr, Textual]
{
	// ABSTRACT	------------------------
	
	override def wrapped: ColorContext
	
	/**
	  * @param base New color context to wrap
	  * @return A copy of this context, wrapping the specified color context
	  */
	def withColorBase(base: ColorContext): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def background: Color = wrapped.background
	override def textColor: Color = wrapped.textColor
	
	override def withDefaultTextColor: Repr = withColorBase(wrapped.withDefaultTextColor)
	override def withTextColor(color: Color): Repr = withColorBase(wrapped.withTextColor(color))
	override def withTextColor(color: ColorSet): Repr = withColorBase(wrapped.withTextColor(color))
	
	override def against(background: Color): Repr = withColorBase(wrapped.against(background))
	
	override def withBase(baseContext: BaseContext): Repr = withColorBase(wrapped.withBase(baseContext))
}
