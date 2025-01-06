package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.{StaticColorContextLike, StaticColorContextWrapper}
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for component creation factories that use a component creation context and allow
  * background drawing
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  */
trait ContextualBackgroundAssignableFactory[N <: StaticColorContextLike[N, _], +Repr <: CustomDrawableFactory[Repr]]
	extends ContextualFactory[N, Repr] with StaticContextualBackgroundAssignable[N, Repr]
		with StaticColorContextWrapper[N, Repr]
{
	// IMPLEMENTED  -------------------------
	
	override def withBackground(background: Color) =
		mapContext { _.against(background) }.withCustomDrawer(BackgroundDrawer(background))
	
	override def withBackground(color: ColorSet, preferredShade: ColorLevel) =
		super[StaticContextualBackgroundAssignable].withBackground(color, preferredShade)
	
	override def withBackground(color: ColorSet) = withBackground(color, Standard)
	override def withBackground(role: ColorRole, preferredShade: ColorLevel) =
		withBackground(context.colors(role), preferredShade)
	override def withBackground(role: ColorRole) = withBackground(role, Standard)
}
