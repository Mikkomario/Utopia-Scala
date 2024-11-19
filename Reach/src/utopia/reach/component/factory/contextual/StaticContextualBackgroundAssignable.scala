package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.StaticColorContextLike
import utopia.paradigm.color.{ColorLevel, ColorSet}

/**
  * Common trait for factories that contain a static context and allow custom background-assigning
  * @author Mikko Hilpinen
  * @since 16.11.2024, v1.5
  * @tparam N Type of context used by this factory (color context or lower)
  * @tparam Repr Type of this factory
  */
trait StaticContextualBackgroundAssignable[+N <: StaticColorContextLike[_, _], +Repr]
	extends Any with ContextualBackgroundAssignable[N, Repr]
{
	// IMPLEMENTED    ------------------------
	
	/**
	  * Applies the best background color for the current context
	  * @param background     Background color set to apply to this component
	  * @param preferredShade The color shade that is preferred (default = Standard)
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorSet, preferredShade: ColorLevel): Repr =
		withBackground(context.color.preferring(preferredShade)(background))
}
