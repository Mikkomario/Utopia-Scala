package utopia.reach.component.factory.contextual

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}

object HasContext
{
	// EXTENSIONS    ---------------------
	
	class FillableFactory[+Repr](val f: CustomDrawableFactory[Repr] with HasContext[ColorContext]) extends AnyVal
	{
		/**
		 * @return Copy of this factory that draws the currently specified background color
		 */
		def drawingBackground = f.withCustomDrawer(BackgroundDrawer(f.context.background))
	}
}

/**
  * Common trait for component factories (and other classes) that wrap a component creation context
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  */
trait HasContext[+N] extends Any
{
	/**
	  * @return The component creation context wrapped by this instance
	  */
	def context: N
}
