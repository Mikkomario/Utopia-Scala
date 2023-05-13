package utopia.reach.component.factory

import utopia.paradigm.color.Color

/**
  * Common trait for factories that allow custom background-assigning
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  * @tparam Repr Type of this factory
  */
trait BackgroundAssignable[+Repr] extends Any
{
	// ABSTRACT -------------------------
	
	/**
	  * @param background A background color to assign for this component
	  * @return Copy of this factory that creates components with that background color
	  */
	def withBackground(background: Color): Repr
}
