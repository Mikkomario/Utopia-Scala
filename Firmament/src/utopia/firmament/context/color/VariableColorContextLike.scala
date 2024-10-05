package utopia.firmament.context.color

import utopia.firmament.context.base.VariableBaseContextLike
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorSet}

/**
  * Common trait for pointer-based color context implementations
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.3.2
  */
trait VariableColorContextLike[+Repr, +Textual]
	extends VariableBaseContextLike[Repr, Repr] with ColorContextCopyable[Repr, Textual]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @param p A pointer that contains the current container / context background color
	  * @return Copy of this context using the specified pointer
	  */
	def withBackgroundPointer(p: Changing[Color]): Repr
	
	/**
	  * @param p A pointer that contains the current text color
	  * @return Copy of this context using the specified pointer
	  */
	def withTextColorPointer(p: Changing[Color]): Repr
	/**
	  * @param p A pointer that contains the current general text color
	  * @return Copy of this context using text colors from the specified pointer
	  */
	def withGeneralTextColorPointer(p: Changing[ColorSet]): Repr
	
	
	// IMPLEMENTED  -------------------------
	
	override def against(backgroundPointer: Changing[Color]): Repr = withBackgroundPointer(backgroundPointer)
	
	override def withTextColor(color: Color): Repr = withTextColorPointer(Fixed(color))
	override def withTextColor(color: ColorSet): Repr = withGeneralTextColorPointer(Fixed(color))
	
	override def mapBackground(f: Color => Color) = mapBackgroundPointer { _.map(f) }
	override def mapTextColor(f: Color => Color) = mapTextColorPointer { _.map(f) }
	
	
	// OTHER   ----------------------------
	
	/**
	  * @param f A mapping function applied to this context's background color pointer
	  * @return Copy of this context with a mapped pointer
	  */
	def mapBackgroundPointer(f: Mutate[Changing[Color]]) = withBackgroundPointer(f(backgroundPointer))
	/**
	  * @param f A mapping function applied to this context's text color pointer
	  * @return Copy of this context with a mapped pointer
	  */
	def mapTextColorPointer(f: Mutate[Changing[Color]]) = withTextColorPointer(f(textColorPointer))
}
