package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.ColorContextPropsView
import utopia.paradigm.color.{ColorLevel, ColorSet}
import utopia.reach.component.factory.VariableBackgroundAssignable

/**
  * Common trait for context-based factories that allow custom variable background-assigning
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  * @tparam N Type of context used by this factory (color context or lower)
  * @tparam Repr Type of this factory
  */
trait ContextualVariableBackgroundAssignable[+N <: ColorContextPropsView, +Repr]
	extends Any with ContextualBackgroundAssignable[N, Repr] with VariableBackgroundAssignable[Repr]
{
	// IMPLEMENTED  -----------------------------
	
	override def withBackground(background: ColorSet, preferredShade: ColorLevel): Repr =
		withBackground(Right(context.colorPointer.preferring(preferredShade)(background)))
}
