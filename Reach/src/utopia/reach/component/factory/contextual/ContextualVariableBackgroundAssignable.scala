package utopia.reach.component.factory.contextual

import utopia.firmament.context.ColorContextLike
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole, ColorSet}
import utopia.reach.component.factory.VariableBackgroundAssignable

/**
  * Common trait for context-based factories that allow custom variable background-assigning
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  * @tparam N Type of context used by this factory (color context or lower)
  * @tparam Repr Type of this factory
  */
trait ContextualVariableBackgroundAssignable[+N <: ColorContextLike[_, _], +Repr]
	extends Any with ContextualBackgroundAssignable[N, Repr] with VariableBackgroundAssignable[Repr]
{
	/**
	  * @param background A variable background color set to use
	  * @param preferredShade Preferred color shade to use, when possible (default = standard)
	  * @return Copy of this factory that uses the specified variable background color,
	  *         picking the best color for the current context
	  */
	def withBackgroundFrom(background: Changing[ColorSet], preferredShade: ColorLevel = Standard) =
		withBackground(background.map { bg => context.color.preferring(preferredShade)(bg) })
	/**
	  * @param background     A variable background color role from which the background colors are picked
	  * @param preferredShade Preferred color shade to use, when possible (default = standard)
	  * @return Copy of this factory that uses a background from the specified variable background color role,
	  *         picking the best color for the current context
	  */
	def withBackgroundRole(background: Changing[ColorRole], preferredShade: ColorLevel = Standard) =
		withBackground(background.map { bg => context.color.preferring(preferredShade)(bg) })
}
