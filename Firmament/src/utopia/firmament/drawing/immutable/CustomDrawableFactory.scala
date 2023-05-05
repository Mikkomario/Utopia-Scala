package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.CustomDrawer

/**
  * Common trait for copyable factories that create items that support custom drawing
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  */
trait CustomDrawableFactory[+Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Custom drawers placed on the created items
	  */
	def customDrawers: Vector[CustomDrawer]
	/**
	  * @param drawers New custom drawers to place
	  * @return Copy of this factory that uses the specified custom drawers
	  */
	def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr
	
	
	// OTHER    ---------------------
	
	/**
	  * @param drawer A new drawer to assign
	  * @return Copy of this factory with the specified drawer included
	  */
	def withCustomDrawer(drawer: CustomDrawer) = withCustomDrawers(customDrawers :+ drawer)
}
