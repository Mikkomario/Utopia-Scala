package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.immutable.Empty

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
	def customDrawers: Seq[CustomDrawer]
	/**
	  * @param drawers New custom drawers to place
	  * @return Copy of this factory that uses the specified custom drawers
	  */
	def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return Copy of this factory without any of the previously assigned custom drawers
	  */
	def withoutCustomDrawers = withCustomDrawers(Empty)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param drawer A new drawer to assign
	  * @return Copy of this factory with the specified drawer included
	  */
	def withCustomDrawer(drawer: CustomDrawer) = withCustomDrawers(customDrawers :+ drawer)
	/**
	  * Attaches a new custom drawer to this factory.
	  * The drawer will be applied before any drawer that has been introduced so far.
	  * @param drawer A new drawer to assign
	  * @return Copy of this factory with the specified drawer included
	  */
	def withCustomBackgroundDrawer(drawer: CustomDrawer) = withCustomDrawers(drawer +: customDrawers)
	/**
	  * @param drawers Custom drawers to add
	  * @return Copy of this factory with the specified custom drawers included
	  */
	def withAdditionalCustomDrawers(drawers: IterableOnce[CustomDrawer]) =
		withCustomDrawers(customDrawers ++ drawers)
}
