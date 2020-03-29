package utopia.reflection.component.drawing.mutable

import utopia.reflection.component.drawing.template.CustomDrawer

/**
  * These components don't perform custom drawing themselves, but wrap another component that does
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait CustomDrawableWrapper extends CustomDrawable
{
	// ABSTRACT	-----------------
	
	/**
	  * @return The wrapped drawable item
	  */
	def drawable: CustomDrawable
	
	
	// IMPLEMENTED	------------
	
	override def customDrawers = drawable.customDrawers
	
	override def customDrawers_=(drawers: Vector[CustomDrawer]) = drawable.customDrawers = drawers
	
	override def drawBounds = drawable.drawBounds
	
	override def repaint() = drawable.repaint()
}
