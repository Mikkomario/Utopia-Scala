package utopia.firmament.context.color

import utopia.firmament.context.base.VariableBaseContextWrapper
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * An implementation of [[VariableColorContext]] by wrapping such an instance
  * @tparam Base Type of wrapped color context implementation
  * @tparam Repr Type of this context implementation
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.4
  */
trait VariableColorContextWrapper[Base <: VariableColorContextLike[Base, Base], +Repr]
	extends VariableBaseContextWrapper[Base, Repr] with ColorContextWrapper2[Base, Repr]
		with VariableColorContextLike[Repr, Repr]
{
	override def withBackgroundPointer(p: Changing[Color]): Repr = mapBase { _.withBackgroundPointer(p) }
	override def withTextColorPointer(p: Changing[Color]): Repr = mapBase { _.withTextColorPointer(p) }
	override def withGeneralTextColorPointer(p: Changing[ColorSet]): Repr = mapBase { _.withGeneralTextColorPointer(p) }
	override def withGeneralBackgroundPointer(p: Changing[ColorSet], preference: ColorLevel) =
		mapBase { _.withGeneralBackgroundPointer(p, preference) }
	override def withBackgroundRolePointer(p: Changing[ColorRole], preference: ColorLevel) =
		mapBase { _.withBackgroundRolePointer(p, preference) }
}