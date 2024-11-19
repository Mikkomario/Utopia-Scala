package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.VariableColorContext
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for variable context -based factories that allow custom background-assigning
  * @author Mikko Hilpinen
  * @since 16.11.2024, v1.5
  * @tparam N Type of context used by this factory (variable color context or lower)
  * @tparam Repr Type of this factory
  */
trait VariableContextualBackgroundAssignable[+N <: VariableColorContext, +Repr]
	extends ContextualVariableBackgroundAssignable[N, Repr]
{
	// OTHER    ---------------------------------
	
	/**
	  * @param background A variable background color set to use
	  * @param preferredShade Preferred color shade to use, when possible (default = standard)
	  * @return Copy of this factory that uses the specified variable background color,
	  *         picking the best color for the current context
	  */
	def withBackgroundFrom(background: Changing[ColorSet], preferredShade: ColorLevel = Standard) =
		withBackground(Right(context.colorPointer.preferring(preferredShade).apply(background)))
	/**
	  * @param background     A variable background color role from which the background colors are picked
	  * @param preferredShade Preferred color shade to use, when possible (default = standard)
	  * @return Copy of this factory that uses a background from the specified variable background color role,
	  *         picking the best color for the current context
	  */
	def withBackgroundRole(background: Changing[ColorRole], preferredShade: ColorLevel = Standard) =
		withBackground(Right(context.colorPointer.preferring(preferredShade).forRole(background)))
}
