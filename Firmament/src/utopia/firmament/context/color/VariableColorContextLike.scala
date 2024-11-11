package utopia.firmament.context.color

import utopia.firmament.context.base.VariableBaseContextLike
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for pointer-based color context implementations
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.4
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
	  * @param p A pointer that contains the color set of the current context
	  * @param preference The preferred color level / shade (default = standard)
	  * @return Copy of this context which applies a background color from the specified set
	  */
	def withGeneralBackgroundPointer(p: Changing[ColorSet], preference: ColorLevel = Standard): Repr
	/**
	  * @param p A pointer that contains the color role of the current context
	  * @param preference The preferred color level / shade (default = standard)
	  * @return Copy of this context which applies a background color applicable to the specified color role
	  */
	def withBackgroundRolePointer(p: Changing[ColorRole], preference: ColorLevel = Standard): Repr
	
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
	
	override def withBackground(color: ColorSet, preferredShade: ColorLevel): Repr =
		withGeneralBackgroundPointer(Fixed(color), preferredShade)
	override def withBackground(role: ColorRole, preferredShade: ColorLevel): Repr =
		withBackgroundRolePointer(Fixed(role), preferredShade)
	
	override def withTextColor(color: Color): Repr = withTextColorPointer(Fixed(color))
	override def withTextColor(color: ColorSet): Repr = withGeneralTextColorPointer(Fixed(color))
	
	override def mapBackground(f: Color => Color) = mapBackgroundPointer { _.map(f) }
	override def mapTextColor(f: Color => Color) = mapTextColorPointer { _.map(f) }
	
	
	// OTHER   ----------------------------
	
	/**
	  * @param f A function that modifies this context's background, possibly yielding variable results
	  * @return Copy of this context with mapped background color
	  */
	def flatMapBackground(f: Color => Changing[Color]) = mapBackgroundPointer { _.flatMap(f) }
	/**
	  * @param f A function that modifies this context's text color, possibly yielding variable results
	  * @return Copy of this context with mapped text color
	  */
	def flatMapTextColor(f: Color => Changing[Color]) = mapTextColorPointer { _.flatMap(f) }
	
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
