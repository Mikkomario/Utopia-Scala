package utopia.firmament.context.color
import utopia.firmament.context.base.StaticBaseContextWrapper
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for implementations of [[StaticColorContext]] by wrapping such an instance
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.4
  */
trait StaticColorContextWrapper[Base <: StaticColorContextLike[Base, _], +Repr]
	extends StaticBaseContextWrapper[Base, Repr] with ColorContextWrapper[Base, Repr]
		with StaticColorContextLike[Repr, Repr]
{
	override def background: Color = base.background
	override def textColor: Color = base.textColor
	
	override def withBackground(color: ColorSet, preferredShade: ColorLevel) =
		super[ColorContextWrapper].withBackground(color, preferredShade)
	override def withBackground(role: ColorRole, preferredShade: ColorLevel) =
		super[ColorContextWrapper].withBackground(role, preferredShade)
	
	override def mapBackground(f: Color => Color) = super[ColorContextWrapper].mapBackground(f)
	override def mapTextColor(f: Color => Color) = super[ColorContextWrapper].mapTextColor(f)
}
