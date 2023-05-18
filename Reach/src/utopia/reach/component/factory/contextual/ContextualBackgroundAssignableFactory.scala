package utopia.reach.component.factory.contextual

import utopia.firmament.context.ColorContextLike
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for component creation factories that use a component creation context and allow
  * background drawing
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  */
trait ContextualBackgroundAssignableFactory[N <: ColorContextLike[N, _], +Repr <: CustomDrawableFactory[Repr]]
	extends ContextualFactory[N, Repr] with ContextualBackgroundAssignable[N, Repr] with ColorContextLike[Repr, Repr]
{
	override def withBackground(background: Color): Repr =
		mapContext { _.against(background) }.withCustomDrawer(BackgroundDrawer(background))
	override def withBackground(color: ColorSet, preferredShade: ColorLevel) =
		super[ContextualBackgroundAssignable].withBackground(color, preferredShade)
	override def withBackground(role: ColorRole, preferredShade: ColorLevel) =
		super[ContextualBackgroundAssignable].withBackground(role, preferredShade)
	override def withBackground(color: ColorSet) = super[ContextualBackgroundAssignable].withBackground(color)
	override def withBackground(role: ColorRole) = super[ContextualBackgroundAssignable].withBackground(role)
}
