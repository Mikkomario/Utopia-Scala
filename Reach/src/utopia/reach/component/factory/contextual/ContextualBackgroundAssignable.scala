package utopia.reach.component.factory.contextual

import utopia.firmament.context.ColorContextLike
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole, ColorSet}
import utopia.reach.component.factory.BackgroundAssignable

/**
  * Common trait for context-based factories that allow custom background-assigning
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  * @tparam N Type of context used by this factory (color context or lower)
  * @tparam Repr Type of this factory
  */
trait ContextualBackgroundAssignable[+N <: ColorContextLike[_, _], +Repr]
	extends Any with BackgroundAssignable[Repr] with HasContext[N]
{
	// OTHER    ------------------------
	
	/**
	  * Applies the best background color for the current context
	  * @param background     Background color set to apply to this component
	  * @param preferredShade The color shade that is preferred (default = Standard)
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorSet, preferredShade: ColorLevel): Repr =
		withBackground(context.color.preferring(preferredShade)(background))
	/**
	  * Applies the best background color for the current context
	  * @param background Background color set to apply to this component
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorSet): Repr = withBackground(background, Standard)
	/**
	  * Applies the best background color for the current context
	  * @param background     Role of the background color to apply to this component
	  * @param preferredShade The color shade that is preferred (default = Standard)
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorRole, preferredShade: ColorLevel): Repr =
		withBackground(context.color.preferring(preferredShade)(background))
	/**
	  * Applies the best background color for the current context
	  * @param background Role of the background color to apply to this component
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorRole): Repr = withBackground(background, Standard)
	
	/*
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
	 */
}
