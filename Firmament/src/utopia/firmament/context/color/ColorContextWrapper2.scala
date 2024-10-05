package utopia.firmament.context.color

import utopia.firmament.context.base.BaseContextWrapper2
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for color context implementations based on wrapping another such instance
  * @tparam Base Wrapped color context implementation
  * @tparam Repr Type of this context implementation
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.3.2
  */
trait ColorContextWrapper2[Base <: ColorContextCopyable[Base, _], +Repr]
	extends BaseContextWrapper2[Base, Repr] with ColorContextCopyable[Repr, Repr]
{
	// IMPLEMENTED  -----------------------------
	
	override def backgroundPointer: Changing[Color] = base.backgroundPointer
	override def textColorPointer: Changing[Color] = base.textColorPointer
	override def hintTextColorPointer: Changing[Color] = base.hintTextColorPointer
	
	override def colorPointer: ColorAccess[Changing[Color]] = base.colorPointer
	
	override def withDefaultTextColor: Repr = mapBase { _.withDefaultTextColor }
	
	override def withTextColor(color: Color): Repr = mapBase { _.withTextColor(color) }
	override def withTextColor(color: ColorSet): Repr = mapBase { _.withTextColor(color) }
	
	override def withBackground(color: ColorSet, preferredShade: ColorLevel): Repr =
		mapBase { _.withBackground(color, preferredShade) }
	override def withBackground(role: ColorRole, preferredShade: ColorLevel): Repr =
		mapBase { _.withBackground(role, preferredShade) }
	
	override def mapBackground(f: Color => Color): Repr = mapBase { _.mapBackground(f) }
	override def mapTextColor(f: Color => Color): Repr = mapBase { _.mapTextColor(f) }
	
	override def against(background: Color): Repr = mapBase { _.against(background) }
}
