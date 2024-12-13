package utopia.reach.component.factory.contextual

import utopia.firmament.context.HasContext
import utopia.firmament.context.base.BaseContextPropsView
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole, ColorSet}
import utopia.reach.component.factory.BackgroundAssignable

/**
  * Common trait for context-based factories that allow custom background-assigning
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  * @tparam N Type of context used by this factory
  * @tparam Repr Type of this factory
  */
trait ContextualBackgroundAssignable[+N <: BaseContextPropsView, +Repr]
	extends Any with BackgroundAssignable[Repr] with HasContext[N]
{
	// ABSTRACT ------------------------
	
	/**
	  * Applies the best background color for the current context
	  * @param background     Background color set to apply to this component
	  * @param preferredShade The color shade that is preferred (default = Standard)
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorSet, preferredShade: ColorLevel): Repr
	
	
	// OTHER    ------------------------
	
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
		withBackground(context.colors(background), preferredShade)
	/**
	  * Applies the best background color for the current context
	  * @param background Role of the background color to apply to this component
	  * @return Copy of this factory with background drawing and modified context
	  */
	def withBackground(background: ColorRole): Repr = withBackground(background, Standard)
}
