package utopia.reach.component.factory

import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color

/**
  * Common trait for factories that allow custom background-assigning based on variable pointer values
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  * @tparam Repr Type of this factory
  */
trait VariableBackgroundAssignable[+Repr] extends Any with BackgroundAssignable[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param background Background color to assign for this component.
	  *                   Either:
	  *                     Left) A static background color, or
	  *                     Right) A variable background color
	  * @return Copy of this factory that uses the specified background color
	  */
	protected def withBackground(background: Either[Color, Changing[Color]]): Repr
	
	
	// IMPLEMENTED  --------------------
	
	/**
	  * @param background A background color to assign for this component
	  * @return Copy of this factory that creates components with that background color
	  */
	override def withBackground(background: Color): Repr = withBackground(Left(background))
	
	
	// OTHER    ------------------------
	
	/**
	  * @param background (Variable) background color to assign
	  * @return Copy of this factory that uses the specified (variable) background color
	  */
	def withBackground(background: Changing[Color]): Repr =
		withBackground(if (background.isFixed) Left(background.value) else Right(background))
}
