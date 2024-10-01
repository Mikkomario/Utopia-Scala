package utopia.firmament.context.color

import utopia.firmament.context.base.{VariableBaseContext, VariableBaseContextWrapper}
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

object VariableColorContext
{
	// ATTRIBUTES   ----------------------
	
	
	
	
	// NESTED   --------------------------
	
	private case class _VariableColorContext(base: VariableBaseContext, backgroundPointer: Changing[Color],
	                                         customTextColorPointer: Option[Either[Changing[Color], Changing[ColorSet]]])
		extends VariableColorContext with VariableBaseContextWrapper[VariableBaseContext, VariableColorContext]
	{
		// IMPLEMENTED  ------------------
		
		override def withBase(base: VariableBaseContext): VariableColorContext = copy(base = base)
		override def withBackgroundPointer(p: Changing[Color]): VariableColorContext =
			copy(backgroundPointer = backgroundPointer)
		
		override def withTextColorPointer(p: Changing[Color]): VariableColorContext = ???
		
		override def withGeneralTextColorPointer(p: Changing[ColorSet]): VariableColorContext = ???
		
		override def withDefaultTextColor: VariableColorContext = ???
		
		override def forTextComponents: VariableColorContext = ???
		
		override def withBackground(color: ColorSet, preferredShade: ColorLevel): VariableColorContext = ???
		
		override def withBackground(role: ColorRole, preferredShade: ColorLevel): VariableColorContext = ???
		
		override def textColorPointer: Changing[Color] = ???
		
		override def hintTextColorPointer: Changing[Color] = ???
		
		override def colorPointer: ColorAccess[Changing[Color]] = ???
		
		override def self: VariableColorContext = ???
		
		override def *(mod: Double): VariableColorContext = ???
	}
}

/**
  * Common trait for variable (i.e. pointer-based) color context implementations.
  * Removes generic type parameters from [[VariableColorContextLike]].
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.3.1
  */
// FIXME: Change textual type once possible
trait VariableColorContext
	extends VariableBaseContext with ColorContext2
		with VariableColorContextLike[VariableColorContext, VariableColorContext]
