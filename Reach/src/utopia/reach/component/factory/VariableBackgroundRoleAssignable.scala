package utopia.reach.component.factory

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}

/**
  * Common trait for component factories that support variable background colors,
  * where the color may be resolved from a color role
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
trait VariableBackgroundRoleAssignable[+Repr] extends BackgroundAssignable[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param pointer A background color pointer.
	  *                Either contains
	  *                     - Left: A pointer to the color role, plus preferred level to apply, or
	  *                     - Right: The resolved background color to apply
	  * @return Copy of this factory that places the specified background color
	  */
	protected def withBackgroundPointer(pointer: Either[(Changing[ColorRole], ColorLevel), Changing[Color]]): Repr
	
	
	// IMPLEMENTED  ----------------------
	
	override def withBackground(background: Color): Repr = withBackgroundPointer(Right(Fixed(background)))
	
	
	// OTHER    --------------------------
	
	/**
	  * @param colorRole Color role to use as the background color
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that places the specified background
	  */
	def withBackground(colorRole: ColorRole, preferredShade: ColorLevel) =
		withBackgroundPointer(Left(Fixed(colorRole), preferredShade))
	/**
	  * @param colorRole Color role to use as the background color
	  * @return Copy of this factory that places the specified background
	  */
	def withBackground(colorRole: ColorRole): Repr = withBackground(colorRole, Standard)
	
	/**
	  * @param p A pointer that determines the background color to place
	  * @return Copy of this factory that uses the specified (variable) background
	  */
	def withBackgroundPointer(p: Changing[Color]): Repr = withBackgroundPointer(Right(p))
	/**
	  * @param p A pointer that determines the background color (role) to use
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that uses the specified (variable) background
	  */
	def withBackgroundRolePointer(p: Changing[ColorRole], preferredShade: ColorLevel = Standard) =
		withBackgroundPointer(Left(p -> preferredShade))
}
